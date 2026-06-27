# Técnicas de los Sistemas Inteligentes (TSI)

Repositorio de las prácticas de la asignatura **Técnicas de los Sistemas Inteligentes**, que explora diferentes paradigmas y técnicas fundamentales en el campo de la Inteligencia Artificial.

## Descripción General

Este repositorio contiene tres prácticas principales que cubren áreas clave de la IA:

1. **Game AI y búsqueda heurística** - Desarrollo de agentes inteligentes para videojuegos
2. **Programación por restricciones** - Resolución de problemas de optimización
3. **Planificación automática** - Generación automática de planes de acción

---

## Estructura del Repositorio

### P1: Agentes basados en búsqueda heurística

**Nota:** 9,59/10

Desarrollo de agentes inteligentes para un entorno de videojuegos utilizando el framework **GVGAI**.
La práctica se centra en comparar distintas técnicas de búsqueda aplicadas a la planificación de rutas en un entorno con obstáculos, recursos, portal de salida y mecánicas especiales como catapultas.

**Lenguaje utilizado:**

* Java

**Algoritmos implementados:**

| Agente              | Técnica  | Tipo de búsqueda                                   |
| ------------------- | -------- | -------------------------------------------------- |
| `AgenteProfundidad` | DFS      | Búsqueda no informada offline                      |
| `AgenteAStar`       | A*       | Búsqueda heurística offline óptima                 |
| `AgenteRTAStar`     | RTA*     | Búsqueda heurística en tiempo real                 |
| `AgenteLRTAStarK`   | LRTA*(k) | Búsqueda en tiempo real con aprendizaje heurístico |

**Contenidos principales:**

* Implementación de agentes para el framework GVGAI.
* Modelado del estado del juego.
* Parseo del mapa: muros, agua, portal, monedas, llaves y catapultas.
* Representación de recursos mediante bitmasks.
* Gestión de mecánicas especiales de vuelo con catapultas.
* Comparación de búsqueda offline y búsqueda en tiempo real.
* Evaluación experimental en varios mapas.
* Recogida de métricas: acciones, nodos expandidos, profundidad máxima, abiertos, cerrados, actualizaciones heurísticas y tiempo de ejecución.

**Resumen de resultados:**

| Algoritmo | Característica principal                                                              |
| --------- | ------------------------------------------------------------------------------------- |
| DFS       | Muy rápido y con bajo consumo de memoria, pero no garantiza optimalidad.              |
| A*        | Encuentra rutas óptimas, aunque expande más nodos y requiere más memoria.             |
| RTA*      | Decide en tiempo real, explorando solo vecinos inmediatos en cada tick.               |
| LRTA*(k)  | Aprende y propaga heurística con `k = 5`, mejorando la convergencia en ciertos mapas. |

**Objetivos de aprendizaje:**

* Comprender la diferencia entre búsqueda informada y no informada.
* Analizar las ventajas e inconvenientes de los algoritmos offline frente a los algoritmos en tiempo real.
* Implementar heurísticas admisibles para guiar la búsqueda.
* Estudiar el impacto de la memoria, el tiempo de ejecución y la calidad de la solución.
* Comparar el comportamiento práctico de DFS, A*, RTA* y LRTA*(k).
* Aplicar técnicas de búsqueda heurística a un entorno de juego realista.

#### Comparativa de algoritmos en P1

En esta práctica se observa una diferencia clara entre los algoritmos offline y los algoritmos en tiempo real.

**DFS** obtiene soluciones muy rápido y con bajo coste computacional, pero el camino encontrado depende mucho del orden de expansión y no tiene por qué ser óptimo.

**A*** es el algoritmo más adecuado cuando se busca la ruta óptima, ya que combina el coste acumulado `g(n)` con una heurística `h(n)`. Su principal inconveniente es el mayor consumo de memoria debido al mantenimiento de las listas de abiertos y cerrados.

**RTA*** toma decisiones tick a tick y solo analiza los vecinos inmediatos del estado actual. Esto lo hace útil en entornos con restricciones temporales, aunque puede producir recorridos más largos al no tener una visión global del problema.

**LRTA\*(k)** incorpora aprendizaje heurístico y propagación acotada con `k = 5`. Esto permite que la información aprendida se difunda por el espacio de estados y mejore progresivamente el comportamiento del agente.



---

### P2: Resolución de problemas de satisfacción de restricciones

**Nota:** 9,63/10

Modelado y resolución de problemas mediante **MiniZinc**, trabajando tanto con problemas de satisfacción de restricciones (**CSP**) como con problemas de optimización con restricciones (**COP**).

La práctica consiste en transformar enunciados complejos en modelos declarativos, definiendo variables de decisión, dominios, restricciones, funciones objetivo y estrategias de búsqueda.

**Lenguaje y herramienta utilizados:**

* MiniZinc
* Solvers de programación por restricciones

**Ejercicios implementados:**

| Ejercicio | Problema                                             | Tipo      |
| --------: | ---------------------------------------------------- | --------- |
|        1a | Fórmula de una bebida isotónica                      | CSP       |
|        1b | Minimización del coste de la bebida isotónica        | COP       |
|         2 | Puzzle lógico de asignación                          | CSP       |
|         3 | Mochila multiobjetivo con optimización lexicográfica | COP       |
|         4 | Cuadrado mágico con restricciones adicionales        | COP       |
|         5 | Planificación de tareas del DeLorean                 | CSP / COP |
|         6 | Conformación de tribunales de defensa de TFG         | CSP       |
|         7 | Planificación de horarios NBA                        | CSP       |

**Contenidos principales:**

* Modelado de problemas mediante variables, dominios y restricciones.
* Uso de restricciones globales como `all_different` y `nvalue`.
* Formulación de problemas CSP y COP.
* Optimización de funciones objetivo.
* Codificación entera para evitar errores de precisión con valores decimales.
* Análisis de escalabilidad al aumentar el tamaño del dominio.
* Rotura de simetrías para reducir soluciones equivalentes.
* Planificación temporal con precedencias y no solapamiento.
* Asignación de recursos con restricciones de compatibilidad.
* Comparación entre enumeración de soluciones y optimización.

**Resultados destacados:**

| Ejercicio | Resultado principal                                                                                                                            |
| --------: | ---------------------------------------------------------------------------------------------------------------------------------------------- |
|         1 | Comparación entre CSP y COP para la fórmula de una bebida isotónica, observando el crecimiento del espacio de búsqueda al aumentar el volumen. |
|         2 | Puzzle lógico resuelto con solución única.                                                                                                     |
|         3 | Mochila multiobjetivo resuelta mediante optimización lexicográfica.                                                                            |
|         4 | Cuadrado mágico con minimización y rotura de simetrías; análisis de escalabilidad para distintos valores de `N`.                               |
|         5 | Planificación óptima del montaje del DeLorean con duración mínima de 28 días.                                                                  |
|         6 | Generación de 4 configuraciones válidas de tribunales de TFG, eliminando simetrías por permutación de tribunales.                              |
|         7 | Obtención de 12 horarios válidos para una semana NBA cumpliendo restricciones de franjas, back-to-backs y partidos fijos.                      |

**Objetivos de aprendizaje:**

* Representar problemas reales como modelos de restricciones.
* Diferenciar entre encontrar una solución factible y demostrar optimalidad.
* Diseñar modelos declarativos legibles y mantenibles.
* Usar restricciones globales para mejorar la expresividad del modelo.
* Analizar la escalabilidad de problemas combinatorios.
* Aplicar técnicas de rotura de simetrías.
* Resolver problemas de planificación, asignación y optimización con MiniZinc.

#### Aspectos técnicos destacados de la P2

En esta práctica se trabajaron varios aspectos importantes de la programación por restricciones:

* En el problema de la bebida isotónica se comparó el comportamiento de un CSP frente a un COP, observando que enumerar todas las soluciones puede ser mucho más costoso que optimizar una función objetivo.
* En el puzzle lógico se usaron restricciones de asignación y `all_different` para garantizar que estudiantes, proyectos, aulas y horarios quedasen vinculados de forma única.
* En la mochila multiobjetivo se utilizó una codificación binaria para decidir qué objetos seleccionar, respetando restricciones de peso, dependencias y compatibilidad.
* En el cuadrado mágico se aplicó rotura de simetrías para reducir soluciones equivalentes bajo rotaciones y reflexiones.
* En la planificación de tareas se modelaron precedencias, duración variable, trabajadores, ayudantes y restricciones de no solapamiento.
* En los tribunales de TFG se eliminaron simetrías imponiendo un orden canónico entre tribunales.
* En la planificación NBA se combinaron restricciones de calendario, sesiones, partidos fijos, back-to-backs y distribución de encuentros por día.


---

### P3: Representación de dominios y resolución de problemas con técnicas de planificación

**Nota:** 10/10

Modelado de problemas de planificación automática mediante **PDDL** y resolución con **Fast Downward / LAMA**.

La práctica consiste en construir dominios y problemas de planificación de complejidad creciente, partiendo de acciones básicas de movimiento y extracción de recursos hasta llegar a dominios con comunidad de personajes, objetos, edificios, restricciones complejas, creación de Uruk-Hai y costes variables.

**Lenguaje y herramientas utilizados:**

* PDDL
* Fast Downward
* LAMA planner
* SAS+ como representación intermedia generada por el planificador

**Ejercicios implementados:**

| Ejercicio | Problema                                    | Aspectos principales                                                                        |
| --------: | ------------------------------------------- | ------------------------------------------------------------------------------------------- |
|         1 | Viajar y extraer recursos                   | Acciones básicas, recursos, personajes, caminos destructibles y análisis de `output.sas`.   |
|         2 | Comunidad reducida y destrucción del Anillo | Movimiento conjunto, objetos obligatorios y efectos condicionales.                          |
|         3 | Ampliación de la Comunidad                  | Representación compacta con `comunidadEn`, análisis de escalabilidad y rotura de simetrías. |
|         4 | Creación de Uruk-Hai                        | Nuevos personajes, edificios, cuantificadores, implicaciones y objetivos existenciales.     |
|         5 | Costes variables                            | Uso de `total-cost`, costes no unitarios y comparación entre longitud de plan y coste real. |

**Contenidos principales:**

* Definición de dominios y problemas en PDDL.
* Modelado de acciones con precondiciones y efectos.
* Uso de constantes, tipos, predicados y funciones.
* Efectos condicionales mediante `when`.
* Uso de cuantificadores `forall` y `exists`.
* Uso de implicaciones para expresar restricciones contextuales.
* Representación compacta del estado para reducir el espacio de búsqueda.
* Rotura de simetrías con predicados auxiliares.
* Análisis de la representación intermedia `output.sas`.
* Planificación con costes unitarios y costes variables.
* Comparación entre planes más cortos y planes de menor coste acumulado.

**Resultados destacados:**

| Ejercicio | Resultado principal                                                                                                                                                                  |
| --------: | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
|         1 | Plan óptimo de 11 pasos para extraer los recursos requeridos.                                                                                                                        |
|         2 | Dominio ampliado para formar una Comunidad reducida y destruir el Anillo siguiendo un flujo obligatorio de recogida de objetos.                                                      |
|         3 | Comparación de varias configuraciones de Comunidad; las configuraciones pequeñas se resuelven en milisegundos, mientras que la variante con Elfo dispara el tiempo de planificación. |
|         4 | Plan válido de 45 pasos para crear Uruk-Hai combinando edificios, recursos, personajes malignos y transporte de Especia.                                                             |
|         5 | Introducción de costes variables: el planificador prefiere una ruta con más pasos cuando su coste acumulado es menor.                                                                |

**Objetivos de aprendizaje:**

* Comprender cómo se representa un problema de planificación clásica.
* Diseñar dominios PDDL reutilizables y progresivos.
* Analizar el impacto de la representación sobre el rendimiento del planificador.
* Reducir el espacio de búsqueda mediante modelado compacto.
* Aplicar rotura de simetrías para evitar planes equivalentes.
* Usar cuantificadores e implicaciones para expresar restricciones complejas.
* Interpretar la traducción de PDDL a SAS+.
* Diferenciar entre minimizar longitud del plan y minimizar coste acumulado.

#### Aspectos técnicos destacados de la P3

En esta práctica se observa especialmente la importancia del diseño de la representación.

En los primeros ejercicios se parte de un dominio básico con acciones como `Viajar` y `ExtraerRecurso`, incorporando recursos, personajes y caminos destructibles. El análisis de `output.sas` permite ver cómo Fast Downward traduce los predicados PDDL a variables SAS+ y operadores internos.

En el ejercicio de la Comunidad, el uso inicial de posiciones individuales para cada miembro funciona correctamente, pero no escala bien. Por eso, en el ejercicio 3 se introduce el predicado `comunidadEn`, que representa la posición conjunta del grupo y reduce el número de combinaciones de estados que debe explorar el planificador.

También se aplica rotura de simetrías con `hobbitAntes`, evitando que el planificador explore planes equivalentes generados por permutar Hobbits dentro de la Comunidad.

En el ejercicio 4 se amplía el dominio con nuevos personajes, edificios y acciones. Se usan construcciones avanzadas de PDDL como `forall`, `exists` e `imply` para expresar restricciones de construcción, extracción y objetivos existenciales sin crear acciones adicionales innecesarias.

Finalmente, en el ejercicio 5 se introducen costes variables mediante `total-cost`. Esto permite que el planificador no busque simplemente el plan con menos acciones, sino el plan con menor coste real, incluso si eso implica recorrer más pasos.

---

## 🚀 Cómo Usar este Repositorio

### **Para P1 (GVGAI)**
```bash
cd P1/GVGAI-master
# Seguir instrucciones del framework GVGAI
# Compilar y ejecutar los agentes desarrollados
```

### **Para P2 (MiniZinc)**
```bash
cd P2
# Utilizar un solucionador de MiniZinc (e.g., MiniZinc IDE, Chuffed)
# Ejecutar: minizinc Ejercicio1a.mzn
```

### **Para P3 (PDDL)**
```bash
cd P3
# Utilizar un planificador PDDL (e.g., FF, LAMA, Metric-FF)
# Ejecutar: solver dominio1.pddl problema1.pddl
```

---

## Conceptos Clave

### **P1: Búsqueda y Juegos**
- Algoritmos de búsqueda informada (A*, RTA*, LRTA*(k))
- Algoritmos adversariales (minimax, alpha-beta pruning)
- Evaluación heurística en espacios de juego

### **P2: Programación por Restricciones**
- Variables de decisión y dominios
- Restricciones globales
- Estrategias de búsqueda y backtracking
- Optimización con criterios múltiples

### **P3: Planificación Automática**
- Representación de estados y acciones
- Grafos de planificación
- Búsqueda en espacios de planes
- Heurísticas para planificación (h_max, h_add, h_FF)

---

## 👤 Autor

**Joaquín Cruz Lorenzo** - Prácticas desarrolladas como parte del curso de la asignatura Técnicas de los Sistemas Inteligentes.

---

## 📖 Referencias Útiles

- **GVGAI Framework**: http://www.gvgai.net/
- **MiniZinc Documentation**: https://www.minizinc.org/doc-2.7.3/
- **PDDL Specification**: http://www.cs.cmu.edu/~afs/cs.cmu.edu/project/TCA/ftp/papers/kukluskiOoreilly94.pdf
- **Planning.Domains**: http://planning.domains/ - Repositorio de dominios PDDL

---

**Última actualización:** Junio 2026

⭐ Si este repositorio te fue útil, considera marcar con una estrella!
