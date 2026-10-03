# GanadoPro 🐄
**Sistema Integral de Gestión Ganadera** — Versión 1.0

---

## Estructura del Proyecto

```
GanadoPro/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/ganadopro/
│   │   ├── data/
│   │   │   ├── dao/          (AnimalDao, VacunaDao, HistorialMedicoDao, ...)
│   │   │   ├── database/     (GanaderaDatabase.kt)
│   │   │   └── entities/     (Animal, Vacuna, HistorialMedico, ...)
│   │   ├── repository/       (GanaderaRepository.kt)
│   │   ├── ui/
│   │   │   ├── LoginActivity.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── dashboard/    (DashboardFragment)
│   │   │   ├── animales/     (AnimalesFragment, DetalleAnimalFragment, FormAnimalFragment, Adapter)
│   │   │   ├── historial/    (HistorialFragment, Adapter)
│   │   │   ├── vacunas/      (VacunasFragment, Adapter)
│   │   │   ├── alimentacion/ (AlimentacionFragment, Adapter)
│   │   │   ├── peso/         (PesoFragment, Adapter)
│   │   │   ├── produccion/   (ProduccionFragment, Adapter)
│   │   │   ├── reproduccion/ (ReproduccionFragment, Adapter)
│   │   │   ├── ventas/       (VentasFragment, Adapter)
│   │   │   └── reportes/     (ReportesFragment)
│   │   └── viewmodel/        (Un ViewModel por módulo)
│   └── res/
│       ├── layout/           (Todos los XML de pantallas, items, dialogs)
│       ├── navigation/       (nav_graph.xml)
│       ├── menu/             (bottom nav + búsqueda)
│       ├── drawable/         (íconos y fondos)
│       └── values/           (colors, strings, themes)
```

---

## Cómo abrir en Android Studio

1. Descomprime el ZIP
2. Abre **Android Studio** → *Open an existing project*
3. Selecciona la carpeta `GanadoPro/`
4. Espera que Gradle sincronice las dependencias
5. Conecta un dispositivo Android (API 24+) o usa un emulador
6. Presiona ▶ **Run**

---

## Credenciales de acceso (Login)

| Campo    | Valor       |
|----------|-------------|
| Usuario  | `admin`     |
| Contraseña | `ganadopro` |

> Puedes cambiarlas en `LoginActivity.kt`

---

## Módulos implementados

| Módulo              | RF    | Estado |
|---------------------|-------|--------|
| Login + sesión      | —     | ✅ |
| Dashboard           | —     | ✅ |
| Gestión de Animales | RF-01 | ✅ |
| Historial Médico    | RF-02 | ✅ |
| Vacunación          | RF-03 | ✅ |
| Reproducción        | RF-04 | ✅ |
| Alimentación        | RF-05 | ✅ |
| Control de Peso     | RF-06 | ✅ |
| Producción Lechera  | RF-07 | ✅ |
| Ventas y Gastos     | RF-08 | ✅ |
| Reportes + Gráficas | RF-09 | ✅ |
| Notificaciones (vacunas y partos próximos) | — | ✅ |

---

## Reglas de negocio (dónde vive cada una)

- **Vacuna pendiente**: su próxima dosis ya llegó, el animal está Activo y no
  hay un registro posterior de la misma vacuna para ese animal
  (`VacunaDao.getPendientes`). Desde *Dashboard → Vacunas* se toca la pendiente
  y se elige **Registrar dosis aplicada**; al guardarla desaparece de la lista.
- **Parto próximo**: último servicio (Monta/Inseminación) + 283 días, solo para
  hembras activas. Se descarta si después se registró un Parto o un Diagnóstico
  de Preñez con resultado negativo/vacía (`ReproduccionUtils`). El Dashboard y
  las notificaciones usan esta misma función.
- **Venta**: al registrar una venta el animal pasa a estado *Vendido*; al
  eliminar la venta vuelve a *Activo* (`GanaderaRepository.insertVenta` /
  `deleteVenta`).
- **Eliminar un animal** borra también todos sus registros (vacunas, pesos,
  producción, reproducción, alimentación y ventas). Para un animal vendido o
  fallecido es mejor cambiar su estado.
- **Fechas**: siempre texto `yyyy-MM-dd` (`util/Extensiones.kt`).

---

## Dependencias principales

- **Room** — Base de datos local SQLite (con migraciones reales 2→3 y 3→4)
- **Navigation Component + Safe Args** — Navegación entre fragments
- **ViewModel + LiveData** — Arquitectura MVVM
- **WorkManager** — Revisión diaria de alertas
- **Glide** — Carga de imágenes
- **MPAndroidChart** — Gráficas de pastel y barras
- **Material Components** — UI

---

## Futuras versiones (v2.0)

- Sincronización con Firebase
- Exportar a PDF/Excel
- Escaneo QR por animal
- Modo oscuro
- Multiusuario
