package com.example.repartocobro.ui

data class TermSection(val title: String, val content: String)

val FullTermsAndConditions = listOf(
    TermSection(
        "1. Aceptación de los Términos",
        "Al descargar, instalar o utilizar la aplicación Reco (\"la Aplicación\"), usted (\"el Usuario\") acepta estar sujeto a los presentes Términos y Condiciones de uso (\"los Términos\"). Si no está de acuerdo con alguno de estos términos, no deberá utilizar la Aplicación.\n\nLa aceptación de estos Términos es obligatoria y se realiza de manera explícita mediante la casilla de verificación presentada en la pantalla de inicio de sesión. Sin esta aceptación, el acceso a las funcionalidades de la Aplicación quedará restringido."
    ),
    TermSection(
        "2. Descripción del Servicio",
        "Reco es una aplicación móvil para dispositivos Android diseñada como herramienta interna de gestión para el seguimiento de rutas de reparto y cobranza de productos. La Aplicación permite:\n\n• Gestión de Reparto: Registro de cantidades entregadas de productos configurables en las tiendas asignadas a cada cobrador.\n• Gestión de Cobro: Registro de cantidades vendidas, cálculo automático de montos, marcado de tiendas como cobradas o con deuda pendiente (fiado), y cobro posterior de deudas.\n• Panel de Administración: Gestión dinámica de productos, cobradores y tiendas.\n• Estadísticas del Día: Panel con indicadores de progreso, recaudo, deudas pendientes y desempeño por tienda.\n• Exportación de Reportes: Generación de documentos PDF con resumen detallado de cobranza, estadísticas visuales (gráficos y progreso) y guardado en la carpeta Descargas del dispositivo.\n• Respaldo en Google Drive: Subida automática de los reportes PDF a la carpeta \"Reco_Reportes\" de la cuenta de Google del Usuario, previo inicio de sesión con Google.\n• Control de Acceso: Sistema de licenciamiento quincenal mediante códigos de activación validados en línea.\n\nImportante: La Aplicación es una herramienta de registro y apoyo operativo. No constituye un sistema contable oficial y no sustituye la verificación física de mercancía ni los registros contables formales del negocio."
    ),
    TermSection(
        "3. Licencia de Uso y Acceso",
        "El acceso a la Aplicación está sujeto a las siguientes condiciones de licenciamiento:\n\n• Modelo de licencia: Quincenal, mediante código de activación.\n• Duración por código: 15 días calendario (se suman a la fecha de vencimiento vigente, si aplica).\n• Formato del código: Serial alfanumérico de 12 caracteres (ej: A7K2M9X4P1B3).\n• Uso del código: Un solo uso — no reutilizable.\n• Transferibilidad: Intransferible — uso personal del titular.\n• Validación: En línea contra el servicio Supabase al activar el código.\n• Conexión requerida: Internet únicamente para activar o extender la licencia.\n\nUna vez activada la licencia, la Aplicación funciona sin conexión a internet para las operaciones de reparto y cobro. Al expirar la licencia, el registro de cobros y deudas quedará bloqueado hasta ingresar un nuevo código válido.\n\nEl uso indebido, distribución no autorizada o falsificación de códigos de licencia puede resultar en la suspensión permanente del servicio sin derecho a reembolso."
    ),
    TermSection(
        "4. Funcionalidades y Alcance",
        "Con una licencia activa, el Usuario PUEDE:\n\n• Administrar de forma dinámica los productos, precios bases, cobradores y tiendas.\n• Seleccionar su perfil de cobrador/repartidor asignado.\n• Registrar cantidades entregadas y vendidas por tienda.\n• Marcar tiendas como cobradas, registrar deudas (fiado) y cobrar deudas pendientes.\n• Registrar observaciones o novedades por tienda.\n• Consultar estadísticas del día y resúmenes de la ruta.\n• Generar reportes PDF y compartirlos desde el dispositivo.\n• Reiniciar la ruta para iniciar un nuevo día operativo.\n• Consultar y extender el estado de su licencia.\n\nEl Usuario NO PUEDE:\n\n• Registrar ventas superiores a las cantidades entregadas.\n• Editar registros de tiendas ya marcadas como cobradas en el día actual.\n• Transferir su licencia a terceros.\n• Exportar o respaldar la base de datos completa de la Aplicación en formatos crudos sin usar los medios provistos (PDF/Drive)."
    ),
    TermSection(
        "5. Responsabilidades del Usuario",
        "El Usuario se compromete a:\n\n• Veracidad de datos: Ingresar información precisa sobre entregas, ventas, cobros y deudas.\n• Custodia del código: Mantener la confidencialidad de su código de licencia.\n• Uso adecuado: Utilizar la Aplicación exclusivamente para gestión de reparto y cobro.\n• Respaldo de reportes: Generar los PDF antes de reiniciar la ruta; el respaldo en Google Drive requiere iniciar sesión con la cuenta correspondiente.\n• Dispositivo: Mantener un dispositivo Android compatible y en buen estado de funcionamiento.\n\nAl reiniciar la ruta se eliminan de forma permanente las entregas, ventas, cobros del día y observaciones del turno actual. Las deudas pendientes registradas en tiendas pueden conservarse y seguir visibles hasta ser cobradas. Esta acción no se puede deshacer."
    ),
    TermSection(
        "6. Privacidad y Protección de Datos",
        "La Aplicación maneja los siguientes tipos de datos:\n\na) Datos locales (en el dispositivo):\n• Perfil del cobrador seleccionado.\n• Cantidades entregadas, vendidas y estado de cobro por tienda.\n• Deudas pendientes, observaciones y fechas de operación.\n• Estado de licencia, fecha de vencimiento y aceptación de estos Términos.\n• Referencia al último PDF generado.\n\nb) Datos remotos (Supabase):\n• Código de licencia ingresado y su estado de uso (usado/disponible).\n\nLa comunicación con Supabase es exclusivamente para validar y marcar códigos de licencia. No se envían al servidor datos operativos de reparto ni cobro.\n\nc) Respaldo en Google Drive:\n• Al generar un reporte PDF, la Aplicación puede solicitar inicio de sesión con Google para subir el archivo a la carpeta \"Reco_Reportes\" de la cuenta del Usuario.\n• Solo se transfieren los archivos PDF generados por el propio Usuario; no se accede a otros archivos de su Drive.\n\nPermisos: La Aplicación declara permiso de Internet. El acceso a Google Drive se gestiona mediante el flujo oficial de inicio de sesión de Google. No solicita acceso a cámara, micrófono, ubicación ni contactos."
    ),
    TermSection(
        "7. Productos y Precios",
        "La Aplicación gestiona productos con precios unitarios bases configurables:\n\n• El Usuario, desde la sección de Administración, puede crear los productos que desee y establecerles el precio unitario correspondiente.\n\nLos montos de cobro, deuda y reportes PDF se calculan automáticamente con base en estos valores definidos por el Usuario. Una vez el Usuario haya definido y creado productos, estos se añadirán dinámicamente a todos los cálculos y reportes de la Aplicación."
    ),
    TermSection(
        "8. Propiedad Intelectual",
        "La Aplicación Reco, incluyendo su código fuente, diseño de interfaz, logotipo, nombre comercial y documentación, es propiedad exclusiva de LuisPG.\n\nQueda prohibido copiar, modificar, distribuir, realizar ingeniería inversa, sublicenciar o utilizar la marca \"Reco\" sin autorización escrita del titular."
    ),
    TermSection(
        "9. Limitación de Responsabilidad",
        "La Aplicación se proporciona \"tal cual\" y \"según disponibilidad\", sin garantías expresas o implícitas. El desarrollador no será responsable por:\n\n• Pérdida de datos locales por reinicio de ruta, fallos del dispositivo o desinstalación.\n• Fallos al subir PDFs a Google Drive por falta de sesión, conectividad o políticas de la cuenta Google.\n• Interrupciones del servicio de validación de licencias (Supabase) o mantenimiento de terceros.\n• Errores en cálculos derivados de datos incorrectos ingresados por el Usuario.\n• Incompatibilidades con versiones específicas de Android o dispositivos particulares.\n\nEl Usuario utiliza la Aplicación bajo su propia responsabilidad."
    ),
    TermSection(
        "10. Suspensión y Terminación",
        "El desarrollador puede suspender o terminar el acceso, sin previo aviso, en casos de uso fraudulento de licencias, manipulación no autorizada del sistema, violación de estos Términos o decisión de descontinuar la Aplicación.\n\nEn caso de terminación, no habrá derecho a reembolso de licencias no consumidas ni recuperación de datos almacenados en el dispositivo."
    ),
    TermSection(
        "11. Modificaciones",
        "El desarrollador puede modificar estos Términos en cualquier momento. Los cambios serán efectivos al publicarse dentro de la Aplicación.\n\nEl uso continuado de la Aplicación después de la publicación de cambios constituye aceptación tácita de los Términos modificados. Se recomienda revisarlos periódicamente."
    ),
    TermSection(
        "12. Legislación Aplicable",
        "Estos Términos se rigen por las leyes de la República de Colombia. Cualquier controversia derivada del uso de la Aplicación se someterá a la jurisdicción de los tribunales competentes de Colombia."
    ),
    TermSection(
        "13. Contacto",
        "Para consultas, reclamaciones o solicitudes relacionadas con estos Términos o el funcionamiento de la Aplicación:\n\n• Desarrollador: LuisPG\n• Aplicación: Reco — Reparto & Cobro\n• Año: 2026"
    )
)
