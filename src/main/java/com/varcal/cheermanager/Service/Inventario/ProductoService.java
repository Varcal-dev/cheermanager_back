package com.varcal.cheermanager.Service.Inventario;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Inventario.ProductoDTO;
import com.varcal.cheermanager.DTO.Inventario.ProductoResponseDTO;
import com.varcal.cheermanager.DTO.Inventario.ProductoVarianteDTO;
import com.varcal.cheermanager.DTO.Inventario.ProductoVarianteResponseDTO;
import com.varcal.cheermanager.Models.Inventario.CategoriaProducto;
import com.varcal.cheermanager.Models.Inventario.Producto;
import com.varcal.cheermanager.Models.Inventario.ProductoVariante;
import com.varcal.cheermanager.repository.Inventario.CategoriaProductoRepository;
import com.varcal.cheermanager.repository.Inventario.ProductoRepository;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    @Autowired
    private ProductoVarianteService productoVarianteService;

    public List<ProductoResponseDTO> listarTodos() {
        return productoRepository.findAll().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    // Sobrecarga con búsqueda: si "q" viene vacío/nulo se comporta exactamente
    // igual que listarTodos() (compatibilidad con quien ya llame al método
    // original). Si trae texto, filtra en la BD por nombre, SKU o categoría.
    public List<ProductoResponseDTO> listarTodos(String q) {
        if (q == null || q.isBlank()) {
            return listarTodos();
        }
        return productoRepository.buscar(q.trim()).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    public List<ProductoResponseDTO> listarActivos() {
        return productoRepository.findByActivoTrue().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    public ProductoResponseDTO obtenerPorId(Integer id) {
        return toResponseDTO(buscarOFallar(id));
    }

    public Producto crear(ProductoDTO dto) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(dto.getCategoriaProductoId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + dto.getCategoriaProductoId()));

        String codigoArticulo = validarCodigoArticulo(dto.getCodigo());
        String sku = "RAT-" + categoria.getCodigo() + "-" + codigoArticulo;

        if (productoRepository.existsBySku(sku)) {
            throw new RuntimeException("Ya existe un producto con el SKU '" + sku + "'");
        }

        Producto producto = new Producto();
        producto.setSku(sku);
        aplicarDatos(producto, dto, categoria);
        producto = productoRepository.save(producto);

        // Todo producto, aunque sea "talla única" (ej. sticker pack), debe
        // tener al menos UNA ProductoVariante — es el único lugar donde vive
        // el stock real (ver comentario en ProductoVariante.java). Sin esto,
        // el producto no aparece para venta (ver ventas/page.tsx) y no
        // muestra stock en el listado hasta que alguien entre manualmente a
        // crearle una variante.
        //
        // Solo se puede auto-crear si hay un precio de referencia con qué
        // arrancar (precioVenta es obligatorio en ProductoVariante). Si el
        // producto se creó sin ningún precio de referencia, se deja sin
        // variante — el usuario tendrá que crear la primera manualmente
        // desde el detalle del producto e indicar el precio de venta real.
        java.math.BigDecimal precioInicial = dto.getPrecioMaxReferencia() != null
                ? dto.getPrecioMaxReferencia()
                : dto.getPrecioMinReferencia();

        if (precioInicial != null) {
            ProductoVarianteDTO varianteDto = new ProductoVarianteDTO();
            varianteDto.setProductoId(producto.getId());
            varianteDto.setTalla(null);
            varianteDto.setColor(null);
            varianteDto.setPrecioVenta(precioInicial);
            varianteDto.setStockInicial(0);
            productoVarianteService.crear(varianteDto);
        }

        // Se vuelve a leer para que la respuesta (y cualquier toResponseDTO
        // posterior) incluya la variante recién creada.
        return buscarOFallar(producto.getId());
    }

    public Producto actualizar(Integer id, ProductoDTO dto) {
        Producto producto = buscarOFallar(id);
        CategoriaProducto categoria = categoriaProductoRepository.findById(dto.getCategoriaProductoId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + dto.getCategoriaProductoId()));

        // El SKU no se regenera al editar, aunque cambie de categoría — es un
        // identificador estable una vez creado (evita romper etiquetas ya
        // impresas o referencias en ventas históricas).
        aplicarDatos(producto, dto, categoria);
        return productoRepository.save(producto);
    }

    public void desactivar(Integer id) {
        Producto producto = buscarOFallar(id);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    // No se hace DELETE físico: un producto con historial de ventas no puede
    // desaparecer sin dejar huérfanas las ventas pasadas. Se desactiva.
    public void eliminar(Integer id) {
        desactivar(id);
    }

    private void aplicarDatos(Producto producto, ProductoDTO dto, CategoriaProducto categoria) {
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setCategoriaProducto(categoria);
        producto.setPrecioMinReferencia(dto.getPrecioMinReferencia());
        producto.setPrecioMaxReferencia(dto.getPrecioMaxReferencia());
        producto.setOrigen(dto.getOrigen());
        producto.setNotasProveedor(dto.getNotasProveedor());
        producto.setRequiereControlStock(dto.getRequiereControlStock() != null ? dto.getRequiereControlStock() : Boolean.TRUE);
        producto.setActivo(true);
    }

    private String validarCodigoArticulo(String codigo) {
        if (codigo == null || !codigo.matches("[A-Za-z]{3}")) {
            throw new RuntimeException("El código de artículo debe tener exactamente 3 letras (ej. CAE, ZAP, MON)");
        }
        return codigo.toUpperCase();
    }

    private Producto buscarOFallar(Integer id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
    }

    private ProductoResponseDTO toResponseDTO(Producto p) {
        ProductoResponseDTO dto = new ProductoResponseDTO();
        dto.setId(p.getId());
        dto.setSku(p.getSku());
        dto.setNombre(p.getNombre());
        dto.setDescripcion(p.getDescripcion());
        dto.setCategoriaProductoId(p.getCategoriaProducto().getId());
        dto.setCategoriaNombre(p.getCategoriaProducto().getNombre());
        dto.setPrecioMinReferencia(p.getPrecioMinReferencia());
        dto.setPrecioMaxReferencia(p.getPrecioMaxReferencia());
        dto.setOrigen(p.getOrigen());
        dto.setNotasProveedor(p.getNotasProveedor());
        dto.setRequiereControlStock(p.getRequiereControlStock());
        dto.setActivo(p.getActivo());

        List<ProductoVariante> variantes = p.getVariantes();
        if (variantes != null) {
            dto.setVariantes(variantes.stream().map(this::toVarianteDTO).collect(Collectors.toList()));
            if (Boolean.TRUE.equals(p.getRequiereControlStock())) {
                dto.setStockTotal(variantes.stream()
                        .mapToInt(v -> v.getStockActual() != null ? v.getStockActual() : 0)
                        .sum());
            }
        }
        return dto;
    }

    private ProductoVarianteResponseDTO toVarianteDTO(ProductoVariante v) {
        ProductoVarianteResponseDTO dto = new ProductoVarianteResponseDTO();
        dto.setId(v.getId());
        dto.setProductoId(v.getProducto().getId());
        dto.setNombreProducto(v.getProducto().getNombre());
        dto.setTalla(v.getTalla());
        dto.setColor(v.getColor());
        dto.setSkuVariante(v.getSkuVariante());
        dto.setPrecioVenta(v.getPrecioVenta());
        dto.setStockActual(v.getStockActual());
        dto.setActivo(v.getActivo());
        return dto;
    }
}