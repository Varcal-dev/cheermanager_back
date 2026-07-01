# Módulo EvaluacionRutina — rúbrica FEDECOLCHEER 2026

Motor de evaluación de rutinas que replica el United Scoring System de
FEDECOLCHEER/IASF, con tablas de cantidad y topes editables desde el
sistema y cálculo automático de cada sub-criterio a partir de los datos
crudos del juez.

## Archivos nuevos

**Modelos** (Models/Evaluaciones/Rutina/):
NivelCompetencia, SubCriterioRubrica, TablaCantidadNivel, TopeSeccionNivel,
DriverSubCriterio, TopeDriverNivel, EscalonDriverNivel, EvaluacionRutina,
RegistroSubCriterio, RegistroDeduccionDriver, RegistroDriverSubCriterio.

**Repositories** (repository/Evaluaciones/Rutina/):
Un repository por cada modelo arriba.

**Calculadoras** (Service/Evaluaciones/Rutina/calculadoras/):
- CalculadoraSubCriterio.java (interfaz)
- CalculadoraEscalonHabilidadesGrupos.java  → Elevaciones + Pirámides
- CalculadoraEscalonCantidadSimple.java     → Saltos + Lanzamientos
- CalculadoraEscalonGimnasia.java           → Gimnasia Estática + con Carrera
- CalculadoraValorInicialMenosDrivers.java  → Ejecución (todas las sub-secciones)
- CalculadoraRangoDirectoJuez.java          → Formaciones, Creatividad, Dance, Showmanship

**Services**: EvaluacionRutinaService.java, RubricaConfigService.java
**Controllers**: EvaluacionRutinaController.java, RubricaConfigController.java
**DTOs**: EvaluacionRutinaDTO.java, EvaluacionRutinaResponseDTO.java
**SQL**: rubrica_datos_iniciales.sql (niveles N1-N4.2 con todos los valores de la rúbrica 2026)

## Endpoints principales

POST /api/evaluaciones-rutina              → registrar evaluacion completa (calcula automatico)
GET  /api/evaluaciones-rutina/grupo/{id}   → historial de un grupo
GET  /api/evaluaciones-rutina/{id}         → ver con desglose completo
GET  /api/rubrica-config/niveles           → ver niveles cargados
POST /api/rubrica-config/niveles           → agregar nivel nuevo (ej. Prep 1.1)
POST /api/rubrica-config/tabla-cantidad    → cargar fila de tabla
POST /api/rubrica-config/topes-seccion     → cargar escalon de puntaje
PUT  /api/rubrica-config/topes-seccion/{id} → actualizar cuando cambie la rubrica

## Lo que cambia por nivel (BD) vs. fijo en código

Configurable en BD: TablaCantidadNivel, TopeSeccionNivel, TopeDriverNivel, EscalonDriverNivel.
Fijo en código: las fórmulas de cada calculadora (cambian solo si FEDECOLCHEER
cambia la metodología, no cada temporada).
El tope especial 0.1 para Sincronización de Saltos y Altura de Lanzamientos
está fijo en CalculadoraValorInicialMenosDrivers por decisión tuya.

## Permisos nuevos a crear

- crear_evaluacion_rutina (juez/entrenador)
- configurar_rubrica (solo admin)

## Agregar Prep/Novice más adelante

1. INSERT en niveles_competencia ("Prep 1.1", "Novice Tiny", etc.)
2. Cargar sus tablas de cantidad via API (valores distintos a los de Elite)
3. Cargar sus topes de sección (Prep tiene topes menores; Novice Tiny no tiene Elevaciones)
4. El código de las calculadoras NO cambia, solo se agregan filas de config.

## Nota técnica

La integración entre EvaluacionRutinaService y las 5 calculadoras usa
inyección por lista: Spring inyecta todas las implementaciones de
CalculadoraSubCriterio y el service las indexa por getTipo() en un EnumMap.
Para agregar una nueva calculadora en el futuro: implementar la interfaz,
anotarla con @Component, y Spring la registra automáticamente sin tocar
EvaluacionRutinaService.
