# Técnicas de los Sistemas Inteligentes (TSI)

Repositorio de las prácticas de la asignatura **Técnicas de los Sistemas Inteligentes**, que explora diferentes paradigmas y técnicas fundamentales en el campo de la Inteligencia Artificial.

## Descripción General

Este repositorio contiene tres prácticas principales que cubren áreas clave de la IA:

1. **Game AI y búsqueda heurística** - Desarrollo de agentes inteligentes para videojuegos
2. **Programación por restricciones** - Resolución de problemas de optimización
3. **Planificación automática** - Generación automática de planes de acción

---

## Estructura del Repositorio

### **P1: Experimentación con técnicas de búsqueda**
### **Nota: 9,59/10**

Implementación de agentes inteligentes utilizando el **General Video Game AI Framework**.

**Lenguaje que he utilizado:**
- ☕ **Java** 

**Contenidos:**
- Algoritmos de búsqueda (DFS, A*, RTA*, LRTA*(k))
- Desarrollo de agentes basados en juegos
- Evaluación de estrategias de inteligencia artificial en entornos de videojuegos
- Implementación del framework GVGAI

**Objetivos de aprendizaje:**
- Comprender cómo funcionan los algoritmos de búsqueda en espacios de estados
- Diseñar agentes que tomen decisiones óptimas en entornos complejos
- Implementar heurísticas eficientes para la búsqueda

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
