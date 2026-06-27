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

### **P2: Resolución de problemas de satisfacción de restricciones**
### **Nota: 9,63/10**

Resolución de problemas de optimización combinatoria mediante **Constraint Programming (CP)**.

**Entorno de trabajo:**
- **MiniZinc**

**Contenidos:**
- Ejercicio 1a y 1b: Introducción a MiniZinc y modelado de restricciones básicas
- Ejercicio 2-3: Problemas clásicos de optimización
- Ejercicio 4-5a y 5b: Problemas más complejos con múltiples restricciones
- Ejercicio 6-7: Casos avanzados con optimización multicriterio

**Objetivos de aprendizaje:**
- Modelar problemas del mundo real como problemas de satisfacción de restricciones (CSP)
- Utilizar solucionadores de restricciones para encontrar soluciones óptimas
- Comprender la diferencia entre búsqueda heurística y programación por restricciones
- Optimizar sistemas complejos con múltiples restricciones

**Problemas tratados:**
- Problemas de asignación y scheduling
- Optimización de recursos
- Problemas combinatorios clásicos

---

### **P3: Representación de dominios y resolución de problemas con técnicas de planificación**
### **Nota: 10/10**

Desarrollo de soluciones mediante **Planning Domain Definition Language (PDDL)**, el estándar de facto para planificación automática.

**Lenguaje:**
- **PDDL**

**Contenidos:**
- **Dominios (5 dominios)**: Definición de acciones, predicados y restricciones
  - `dominio1.pddl` - `dominio5.pddl`: Progresión de complejidad en dominios de planificación
  
- **Problemas (5 problemas)**: Definición de estados iniciales y objetivos
  - `problema1.pddl` - `problema5.pddl`: Instancias de complejidad creciente

**Objetivos de aprendizaje:**
- Comprender la representación PDDL de dominios y problemas
- Diferenciar entre planificación clásica y planificación con restricciones
- Implementar soluciones escalables para problemas de planificación
- Aprender sobre grafos de planificación y búsqueda en espacios de planes

**Tipos de problemas:**
- Planificación de secuencias de acciones
- Problemas de logística y transporte
- Problemas de manipulación de objetos
- Problemas con restricciones de recursos

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
