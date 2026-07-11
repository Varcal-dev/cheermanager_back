package com.varcal.cheermanager.Service.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Dashboard.ResumenTiendaDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenTiendaDTO.ProductoMasVendidoDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenTiendaDTO.VarianteStockBajoDTO;
import com.varcal.cheermanager.Models.Inventario.DetalleVenta;
import com.varcal.cheermanager.Models.Inventario.ProductoVariante;
import com.varcal.cheermanager.Models.Inventario.Venta;
import com.varcal.cheermanager.repository.Inventario.ProductoVarianteRepository;
import com.varcal.cheermanager.repository.Inventario.VentaRepository;

@Service
public class DashboardTiendaService {

    private static final int UMBRAL_STOCK_BAJO_DEFECTO = 5;
    private static final int TOP_PRODUCTOS = 5;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoVarianteRepository productoVarianteRepository;

    public ResumenTiendaDTO obtenerResumen() {
        ResumenTiendaDTO dto = new ResumenTiendaDTO();

        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());

        List<Venta> ventasDelMes = ventaRepository.findByFechaBetweenOrderByFechaDesc(inicioMes, finMes);

        dto.setVentasMesActual(ventasDelMes.size());
        dto.setIngresoMesActual(ventasDelMes.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add));

        // Se recorren todas las líneas de detalle de las ventas del mes una
        // sola vez, acumulando en paralelo: unidades totales, ingreso por
        // categoría, y ranking por producto — evita recorrer la misma lista
        // tres veces.
        long unidadesTotales = 0;
        Map<String, BigDecimal> ingresoPorCategoria = new HashMap<>();
        Map<String, long[]> unidadesPorProducto = new HashMap<>(); // [unidades] — usar array para acumular dentro de lambda
        Map<String, BigDecimal> ingresoPorProducto = new HashMap<>();

        for (Venta venta : ventasDelMes) {
            List<DetalleVenta> detalles = venta.getDetalles();
            if (detalles == null) continue;

            for (DetalleVenta d : detalles) {
                unidadesTotales += d.getCantidad();

                String categoria = d.getProductoVariante().getProducto().getCategoriaProducto().getNombre();
                ingresoPorCategoria.merge(categoria, d.getSubtotal(), BigDecimal::add);

                String nombreProducto = d.getProductoVariante().getProducto().getNombre();
                unidadesPorProducto.computeIfAbsent(nombreProducto, k -> new long[1])[0] += d.getCantidad();
                ingresoPorProducto.merge(nombreProducto, d.getSubtotal(), BigDecimal::add);
            }
        }

        dto.setUnidadesVendidasMesActual(unidadesTotales);
        dto.setIngresoPorCategoria(ingresoPorCategoria);
        dto.setTopProductosVendidos(construirTopProductos(unidadesPorProducto, ingresoPorProducto));
        dto.setVariantesConStockBajo(construirStockBajo());

        return dto;
    }

    private List<ProductoMasVendidoDTO> construirTopProductos(Map<String, long[]> unidadesPorProducto,
            Map<String, BigDecimal> ingresoPorProducto) {
        return unidadesPorProducto.entrySet().stream()
                .map(entry -> {
                    ProductoMasVendidoDTO item = new ProductoMasVendidoDTO();
                    item.setNombreProducto(entry.getKey());
                    item.setUnidadesVendidas(entry.getValue()[0]);
                    item.setIngresoGenerado(ingresoPorProducto.getOrDefault(entry.getKey(), BigDecimal.ZERO));
                    return item;
                })
                .sorted(Comparator.comparingLong(ProductoMasVendidoDTO::getUnidadesVendidas).reversed())
                .limit(TOP_PRODUCTOS)
                .collect(Collectors.toList());
    }

    private List<VarianteStockBajoDTO> construirStockBajo() {
        List<ProductoVariante> variantes = productoVarianteRepository.findConStockBajo(UMBRAL_STOCK_BAJO_DEFECTO);
        List<VarianteStockBajoDTO> resultado = new ArrayList<>();

        for (ProductoVariante v : variantes) {
            VarianteStockBajoDTO item = new VarianteStockBajoDTO();
            item.setProductoVarianteId(v.getId());
            item.setSkuVariante(v.getSkuVariante());
            item.setNombreProducto(v.getProducto().getNombre());
            item.setTalla(v.getTalla());
            item.setColor(v.getColor());
            item.setStockActual(v.getStockActual());
            resultado.add(item);
        }
        return resultado;
    }

    // NOTA: "rotación de inventario" (unidades vendidas / stock promedio en
    // un periodo) quedó fuera a propósito — el sistema no guarda un
    // histórico de nivel de stock en el tiempo (solo el stock ACTUAL), así
    // que no hay forma de calcular un "promedio" real sin asumir datos que
    // no existen. Si se necesita esta métrica, hay que decidir primero cómo
    // trackear el stock histórico (snapshots periódicos, o derivarlo de
    // MovimientoInventario acumulado día a día).
}