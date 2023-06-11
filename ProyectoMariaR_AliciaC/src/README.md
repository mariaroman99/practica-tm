## Proyecto Práctica
**Componentes del grupo:** Alicia Carrasco y Maria Roman

**Ejecución Proyecto**:

Para la ejecución del proyecto, es necesario  el SDK de Java Oracle 
con la verison 20. También es necesaria la libreria *javafx*. 
El proyecto consta de una carpeta *src* donde se encuentran los archivos:
 - *Main.java*
 - *LectorImagenes.java*
 - *Processador.java*
 - *ReproductorImagenes.java*
 - *Cubo.zip*
 - *CodificadorVideo.java*
 - *DecodificadorVideo.java*
 - *ProgressDemo.java*
 - *Tiles.java*
 - *Imagen.java*

Para ejecutar el proyecto, es necesario entrar a la carpeta *src* y
desde la consola compilar con *javac Main.java* y luego ejecutar con
*java Main args1 args2* con los argumentos pertinentes.

Para codificar y decodificar, es necesario poner en --nTiles el mismo numero de teselas. 

Mejoras para la entrega final:

- Tener contorladas todos los posibles errores de entradas de argumentos
- Mejorar el decodificador y su eficiencia

Aspectos mejorados:

- Tamaño de las teselas iguales para todas
- Eliminación de superposición de teselas

Cuando el Codificador se ejecuta, las imagenes resultantes junto con el fichero de coordenadas
son guardados en la carpeta output/salida.zip/ImagenesComprimidas.
Para el Decodificador, la salida se guarda en el direcotrio output/ImagenesDescompirmidas.zip.