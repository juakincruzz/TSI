package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;

/**
 * Agente basado en Búsqueda en Profundidad (DFS - no informada).
 * Optimizado para el entorno GVGAI (evita bloqueos de llaves, catapultas y Timeouts).
 */
public class AgenteProfundidad extends AbstractPlayer {

    // Aquí guardaremos la secuencia de acciones que nos lleva a la victoria
    private ArrayList<ACTIONS> planDeAccion;

    /**
     * Constructor del agente. Aquí ejecutamos la búsqueda OFFLINE antes de que empiece el juego.
     */
    public AgenteProfundidad(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        planDeAccion = new ArrayList<>();

        System.out.println("Iniciando Búsqueda en Profundidad (DFS)...");

        // Llamamos a nuestro método de búsqueda principal
        Nodo nodoDestino = buscarRutaDFS(stateObs);

        // Si hemos encontrado el portal, reconstruimos el camino
        if (nodoDestino != null) {
            construirPlan(nodoDestino);
            System.out.println("¡Ruta encontrada! Pasos a dar: " + planDeAccion.size());
        } else {
            System.out.println("No se encontró ninguna ruta al portal.");
        }
    }

    /**
     * Algoritmo DFS puro con protección contra Timeout en la simulación.
     */
    private Nodo buscarRutaDFS(StateObservation estadoInicial) {
        // Frontera tipo Pila (LIFO) para la Búsqueda en Profundidad
        Stack<Nodo> frontera = new Stack<>();
        
        // Mapa de visitados: guarda el ID del estado y el COSTE en el que llegamos
        HashSet<String> visitados = new HashSet<>();

        // Creamos el nodo raíz y lo metemos en la frontera
        Nodo raiz = new Nodo(estadoInicial);
        frontera.push(raiz);

        while (!frontera.isEmpty()) {
            
            // Sacamos el último nodo insertado
            Nodo actual = frontera.pop();
            StateObservation estadoActual = actual.estado;

            // A) ¿Hemos ganado o perdido en esta simulación?
            if (estadoActual.isGameOver()) {
                if (estadoActual.getGameWinner() == ontology.Types.WINNER.PLAYER_WINS) {
                    return actual; // ¡Solución encontrada! Devolvemos el nodo meta
                } else {
                    continue; // Morimos o nos quedamos sin tiempo; podamos esta rama
                }
            }

            // B) Control de Visitados INTELIGENTE
            String idEstado = generarIdEstado(estadoActual);
            if (visitados.contains(idEstado)) {
                continue;
            }
            visitados.add(idEstado);
            

            // C) Expansión: Obtener acciones posibles y simular el futuro
            ArrayList<ACTIONS> accionesPosibles = estadoActual.getAvailableActions();
            for (ACTIONS accion : accionesPosibles) {
                
                // NOTA: NO ignoramos ACTION_NIL porque al pisar una catapulta, 
                // el agente sale volando y la única acción permitida durante el vuelo es NIL.
                
                StateObservation estadoHijo = estadoActual.copy();
                estadoHijo.advance(accion); // El motor simula qué pasaría al aplicar la acción

                // Creamos el nodo hijo y lo metemos en la pila
                Nodo hijo = new Nodo(estadoHijo, actual, accion, actual.coste + 1);
                frontera.push(hijo);
            }
        }
        
        return null; // Solo devolverá null si la pila se vacía y es imposible ganar
    }

    private String generarIdEstado(StateObservation estado) {
        tools.Vector2d pos = estado.getAvatarPosition();
        if (pos == null) return "muerto";

        double blockSizeX = estado.getWorldDimension().width / estado.getObservationGrid().length;
        double blockSizeY = estado.getWorldDimension().height / estado.getObservationGrid()[0].length;

        int x = (int) (pos.x / blockSizeX);
        int y = (int) (pos.y / blockSizeY);

        tools.Vector2d ori = estado.getAvatarOrientation();
        String orientacion = (ori != null) ? ((int) ori.x + "_" + (int) ori.y) : "0_0";

        int tipoAvatar = estado.getAvatarType();

        StringBuilder sb = new StringBuilder();
        sb.append("P:").append(x).append(",").append(y);
        sb.append("|O:").append(orientacion);
        sb.append("|T:").append(tipoAvatar);

        // Inventario
        sb.append("|INV:");
        if (estado.getAvatarResources() != null) {
            for (Integer k : estado.getAvatarResources().keySet()) {
                sb.append(k).append("=").append(estado.getAvatarResources().get(k)).append(";");
            }
        }

        // Recursos restantes: monedas, llave...
        sb.append("|R:");
        if (estado.getResourcesPositions() != null) {
            for (int i = 0; i < estado.getResourcesPositions().length; i++) {
                if (estado.getResourcesPositions()[i] != null) {
                    sb.append(i).append("[");
                    for (core.game.Observation obs : estado.getResourcesPositions()[i]) {
                        int rx = (int) (obs.position.x / blockSizeX);
                        int ry = (int) (obs.position.y / blockSizeY);
                        sb.append(rx).append(",").append(ry).append(";");
                    }
                    sb.append("]");
                }
            }
        }

        // Inmovibles restantes: puerta, catapultas, muros...
        sb.append("|I:");
        if (estado.getImmovablePositions() != null) {
            for (int i = 0; i < estado.getImmovablePositions().length; i++) {
                if (estado.getImmovablePositions()[i] != null) {
                    sb.append(i).append("[");
                    for (core.game.Observation obs : estado.getImmovablePositions()[i]) {
                        int ix = (int) (obs.position.x / blockSizeX);
                        int iy = (int) (obs.position.y / blockSizeY);
                        sb.append(ix).append(",").append(iy).append(";");
                    }
                    sb.append("]");
                }
            }
        }

        return sb.toString();
    }
    
    /**
     * Reconstruye el camino desde el nodo final hasta la raíz siguiendo a los padres.
     */
    private void construirPlan(Nodo nodoFinal) {
        Nodo actual = nodoFinal;
        Stack<ACTIONS> pilaAcciones = new Stack<>();
        
        // Subimos por el árbol desde la meta hasta el inicio
        while (actual.padre != null) {
            pilaAcciones.push(actual.accion);
            actual = actual.padre;
        }
        
        // Vaciamos la pila en nuestra lista para que queden en el orden correcto
        while (!pilaAcciones.isEmpty()) {
            planDeAccion.add(pilaAcciones.pop());
        }
    }

    /**
     * Método principal que el motor de GVGAI llama en cada tick del juego.
     */
    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Si tenemos acciones en nuestro plan calculado, ejecutamos la primera y la borramos
        if (planDeAccion != null && !planDeAccion.isEmpty()) {
            return planDeAccion.remove(0);
        }
        
        // Si nos quedamos sin plan, nos quedamos quietos
        return ACTIONS.ACTION_NIL; 
    }
}