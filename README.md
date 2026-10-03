# GanadoPro

App Android para llevar el control de una finca ganadera desde el celular.
Funciona sin internet: todo se guarda en el teléfono.

La hice porque en muchas fincas el registro de los animales todavía se lleva
en cuadernos, y datos como cuándo toca una vacuna o cuándo va a parir una vaca
se terminan perdiendo.

## Qué se puede hacer

- Registrar cada animal con su foto, código, raza, sexo y peso.
- Llevar su historial médico, vacunas, alimentación, pesajes y eventos de
  reproducción.
- Anotar la producción de leche por vaca.
- Registrar ventas y gastos, y ver el balance del mes.
- Ver reportes con gráficas: animales por raza y sexo, leche del mes y en qué
  se va el dinero.
- Recibir un aviso cuando se acerca una vacuna o un parto.

## Cómo probarla

1. Clonar el repositorio y abrir la carpeta `GanadoPro` en Android Studio.
2. Esperar a que sincronice Gradle.
3. Correrla en un emulador o un teléfono con Android 7.0 o superior.

Para entrar: usuario `admin`, contraseña `ganadopro`.

## Con qué está hecha

Kotlin, con arquitectura MVVM. Uso Room para la base de datos, Navigation
Component para moverme entre pantallas, WorkManager para las alertas diarias,
Glide para las fotos y MPAndroidChart para las gráficas.

## Algunas decisiones

- La fecha probable de parto se calcula sumando 283 días al último servicio
  (monta o inseminación). Si después se registra el parto o un diagnóstico
  negativo, la alerta se quita.
- Una vacuna deja de estar pendiente cuando se registra la siguiente dosis.
- Al vender un animal pasa a estado "Vendido" y deja de contar como activo.
- Si se elimina un animal se borra también todo su historial, así que para
  los vendidos o fallecidos es mejor cambiarles el estado.

## Pendiente

- Respaldo en la nube.
- Exportar los reportes a PDF o Excel.
- Varios usuarios.
- Modo oscuro.
