package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tracks.singlePlayer.MetricsProvider;

/**
 * Agente basado en Búsqueda en Profundidad (DFS).
 * Implementa BÚSQUEDA ASÍNCRONA (Time-Bounded) para evitar bloqueos por Timeout.
 * Incluye mapeo topográfico absoluto y memoria de 1 tick para evitar la Poda Ciega en GVGAI.
 */
public class AgenteProfundidad extends AbstractPlayer {

    private ArrayList<ACTIONS> planDeAccion;
    
    // Estructuras de búsqueda globales para mantener la memoria entre turnos
    private Stack<Nodo> frontera;
    private HashMap<String, Integer> visitados;
    
    // Banderas de control de estado
    private boolean buscando;
    private boolean metricasEnviadas;
    
    // Variables para las métricas oficiales de la UGR
    private int nodosExpandidos;
    private int profundidadMaxima;

    /**
     * Constructor del agente.
     * REGLA UGR: SOLO inicialización de estructuras. Cero lógica de búsqueda aquí.
     */
    public AgenteProfundidad(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        super();
        planDeAccion = new ArrayList<>();
        frontera = new Stack<>();
        visitados = new HashMap<>();
        
        buscando = true;
        metricasEnviadas = false;
        nodosExpandidos = 0;
        profundidadMaxima = 0;

        // Metemos el nodo inicial en la frontera
        Nodo raiz = new Nodo(stateObs);
        frontera.push(raiz);
        
        System.out.println("Agente DFS instanciado. Listo para búsqueda asíncrona...");
    }

    /**
     * Método principal que el motor de GVGAI llama en cada tick del juego.
     */
    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        
        // 1. FASE DE BÚSQUEDA (El agente se queda quieto mientras piensa su plan maestro)
        if (buscando) {
            buscarRutaAsincrona(elapsedTimer);
        }

        // 2. FASE DE EJECUCIÓN (El agente ha encontrado la ruta y se mueve rápido)
        if (!buscando && planDeAccion != null && !planDeAccion.isEmpty()) {
            return planDeAccion.remove(0); // Saca y ejecuta la primera acción de la lista
        }
        
        // Mientras piensa o si agotó las acciones, no hace nada (espera)
        return ACTIONS.ACTION_NIL; 
    }

    /**
     * Algoritmo DFS que vigila el tiempo restante de CPU para no ser descalificado.
     */
    private void buscarRutaAsincrona(ElapsedCpuTimer elapsedTimer) {
        Nodo nodoDestino = null;

        while (!frontera.isEmpty()) {
            
            // CONTROL DE TIEMPO ESTRICTO: Si nos quedan menos de 5ms en este tick de GVGAI, 
            // pausamos el cálculo (hacemos return) y continuaremos en el siguiente tick.
            if (elapsedTimer.remainingTimeMillis() < 5) {
                return; 
            }

            Nodo actual = frontera.pop();
            nodosExpandidos++;
            
            // Actualizamos la métrica de profundidad máxima
            if (actual.profundidad > profundidadMaxima) {
                profundidadMaxima = actual.profundidad;
            }

            StateObservation estadoActual = actual.estado;

            // A) ¿Hemos ganado o perdido en esta simulación?
            if (estadoActual.isGameOver()) {
                if (estadoActual.getGameWinner() == ontology.Types.WINNER.PLAYER_WINS) {
                    nodoDestino = actual; // ¡Solución encontrada!
                    break; 
                } else {
                    continue; // Morimos en esta simulación, podamos la rama
                }
            }

            // B) Control de Visitados a prueba de balas
            String idEstado = generarIdEstado(estadoActual, actual.accion);
            
            // Si ya estuvimos aquí, pero en un número MENOR o IGUAL de pasos, podamos la rama
            if (visitados.containsKey(idEstado) && visitados.get(idEstado) <= actual.coste) {
                continue;
            }
            visitados.put(idEstado, actual.coste); // Guardamos el estado y su coste

            // C) Expansión de Nodos
            ArrayList<ACTIONS> accionesPosibles = estadoActual.getAvailableActions();
            for (ACTIONS accion : accionesPosibles) {
                StateObservation estadoHijo = estadoActual.copy();
                estadoHijo.advance(accion); // Simulamos el futuro

                Nodo hijo = new Nodo(estadoHijo, actual, accion, actual.coste + 1);
                frontera.push(hijo);
            }
        }
        
        // Si sale del while es porque encontró la meta o vació toda la pila (se rindió)
        buscando = false; 

        if (nodoDestino != null) {
            construirPlan(nodoDestino);
            System.out.println("¡Ruta encontrada! Pasos a dar: " + planDeAccion.size());
        } else {
            System.out.println("Búsqueda agotada: No se encontró ninguna ruta al portal.");
        }

        // Enviamos las métricas a los profesores una única vez
        if (!metricasEnviadas) {
            MetricsProvider metrics = MetricsProvider.getInstance();
            metrics.setNodosExpandidos(nodosExpandidos);
            metrics.setProfundidadMaxima(profundidadMaxima);
            metrics.setNodosAbiertos(frontera.size());
            metrics.setNodosCerrados(visitados.size());
            metrics.setNumAccionesPlan(nodoDestino != null ? planDeAccion.size() : -1);
            metrics.printMetrics();
            metricasEnviadas = true;
        }
    }

    /**
     * Generador de IDs a prueba de balas para el motor GVGAI.
     * Incluye memoria de 1 tick para físicas retardadas y visión absoluta de proyectiles.
     */
    private String generarIdEstado(StateObservation estado, ACTIONS ultimaAccion) {
        tools.Vector2d pos = estado.getAvatarPosition();
        
        // PARACAÍDAS ANTI-PODAS: Si el avatar está volando en la catapulta, desaparece temporalmente (pos == null).
        if (pos == null) {
            return "oculto_vuelo_" + estado.getGameTick();
        }
        
        StringBuilder id = new StringBuilder();
        
        // 0. Gracia de 1 tick: Evita podar acciones que tardan 1 turno en mostrar su efecto visual
        id.append(ultimaAccion).append("_");
        
        // 1. Posición y orientación continua exacta (evita Aliasing de estado en el aire)
        id.append(pos.x).append("_").append(pos.y).append("_");
        
        tools.Vector2d ori = estado.getAvatarOrientation();
        if (ori != null) id.append(ori.x).append("_").append(ori.y).append("_");
        
        id.append(estado.getAvatarType()).append("_");
        
        // 2. Mapeo estricto de TODOS los objetos (Imprescindible para notar interacciones con bloques/catapultas)
        appendObs(id, estado.getImmovablePositions());
        appendObs(id, estado.getMovablePositions());
        appendObs(id, estado.getResourcesPositions());
        appendObs(id, estado.getNPCPositions());
        appendObs(id, estado.getPortalsPositions());
        
        // ¡LA CLAVE DE CATAPULTS! Leer la lista oculta donde GVGAI guarda los disparos e interacciones
        appendObs(id, estado.getFromAvatarSpritesPositions()); 
        
        // 3. Inventario interno (Recursos)
        if (estado.getAvatarResources() != null) {
            for (Integer key : estado.getAvatarResources().keySet()) {
                id.append(key).append("=").append(estado.getAvatarResources().get(key)).append("_");
            }
        }
        
        return id.toString();
    }

    /**
     * Método auxiliar para registrar el tipo y la posición exacta de cada objeto del juego.
     */
    private void appendObs(StringBuilder sb, java.util.ArrayList<core.game.Observation>[] obsArrays) {
        if (obsArrays != null) {
            for (java.util.ArrayList<core.game.Observation> list : obsArrays) {
                for (core.game.Observation obs : list) {
                    sb.append(obs.itype).append("-").append(obs.position.x).append("-").append(obs.position.y).append("_");
                }
            }
        }
    }

    /**
     * Reconstruye el camino desde el nodo final hasta la raíz siguiendo a los padres.
     */
    private void construirPlan(Nodo nodoFinal) {
        Nodo actual = nodoFinal;
        Stack<ACTIONS> pilaAcciones = new Stack<>();
        
        // Retrocedemos desde la meta hasta el inicio apilando los pasos
        while (actual.padre != null) {
            pilaAcciones.push(actual.accion);
            actual = actual.padre;
        }
        
        // Vaciamos la pila en nuestra lista para que queden en el orden cronológico correcto
        while (!pilaAcciones.isEmpty()) {
            planDeAccion.add(pilaAcciones.pop());
        }
    }
}