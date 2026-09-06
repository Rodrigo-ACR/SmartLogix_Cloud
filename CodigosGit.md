##  Paso a paso para actualizar este proyecto

Sigue esta secuencia exacta cada vez que vayas a trabajar para mantener todo sincronizado:

### Paso 1: Actualizar tu versión local
Descarga siempre la última versión que esté en la nube.
`git pull origin main`

### Paso 2: Preparar los archivos
Una vez que hayas terminado de programar, dile a Git que detecte todos los archivos modificados.
`git add .`

### Paso 3: Crear el punto de guardado (Commit)
Guarda esos cambios con un mensaje descriptivo.
`git commit -m "Descripción de lo que acabo de hacer"`

### Paso 4: Subir los cambios a GitHub
Envía todo tu código nuevo a la rama principal en la nube.
`git push origin main`


---

##  Comandos de utilidad y diagnóstico

Si en algún momento te pierdes o no sabes qué está detectando la consola, estos comandos te salvarán la vida:

### Revisar el estado de tus archivos
Si dudas de si tienes cambios sin guardar o quieres saber qué está pasando en Git:
`git status`

### Listar los archivos y carpetas
Para ver rápidamente qué carpetas están dentro del directorio donde te encuentras (muy útil para confirmar que tus microservicios están ahí):
`dir`

### Navegar entre carpetas (Change Directory)
Para entrar a una carpeta específica (por ejemplo, si quieres entrar al microservicio de pedidos):
`cd pedidos`
*(Para salir de esa carpeta y regresar a la anterior, simplemente usa `cd ..`)*

### Ver en qué rama estás
Si no recuerdas si estás en `main` o en otra rama de trabajo:
`git branch`

### Ver tu historial de guardados
Para ver la lista de todos los `commits` (puntos de guardado) que se han subido al proyecto con sus respectivos mensajes:
`git log`
*(Nota: Si la lista es muy larga y no te deja escribir, simplemente presiona la tecla `Q` para salir)*
