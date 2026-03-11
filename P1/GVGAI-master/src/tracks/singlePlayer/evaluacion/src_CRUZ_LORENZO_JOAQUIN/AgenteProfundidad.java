package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

/**
 * Agente basado en Búsqueda en Profundidad (DFS - no informada).
 */
public class AgenteProfundidad extends AbstractPlayer {

    // Aquí guardaremos la secuencia de acciones que nos lleva a la victoria
    private ArrayList<ACTIONS> planDeAccion;
    
    /**
     * Constructor del agente. Aquí ejecutamos la búsqueda OFFLINE.
     */
    public AgenteProfundidad(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        planDeAccion = new ArrayList<>();
        
        System.out.println("Iniciando Búsqueda en Profundidad (DFS)...");
        
        // Llamamos a nuestro método de búsqueda
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
     * Algoritmo DFS puro.
     */
    private Nodo buscarRutaDFS(StateObservation estadoInicial) {
        // 1. Inicializamos la Frontera (LIFO - Pila) y los Visitados
        Stack<Nodo> frontera = new Stack<>();
        HashSet<String> visitados = new HashSet<>();

        // 2. Creamos el nodo raíz y lo metemos en la frontera
        Nodo raiz = new Nodo(estadoInicial);
        frontera.push(raiz);

        // 3. Bucle principal de búsqueda
        while (!frontera.isEmpty()) {
            
            // Sacamos el último nodo insertado (comportamiento en profundidad)
            Nodo actual = frontera.pop();
            StateObservation estadoActual = actual.estado;

            // A) ¿Hemos ganado o perdido en este estado?
            if (estadoActual.isGameOver()) {
                if (estadoActual.getGameWinner() == ontology.Types.WINNER.PLAYER_WINS) {
                    return actual; // ¡Encontramos el portal de salida!
                } else {
                    continue; // Si hemos muerto (ej: caímos al agua), descartamos este camino y seguimos buscando
                }
            }

            // B) Control de Visitados
            String idEstado = generarIdEstado(estadoActual);
            if (visitados.contains(idEstado)) {
                continue; // Si ya hemos estado aquí en las mismas condiciones, lo saltamos
            }
            visitados.add(idEstado); // Lo marcamos como visitado

            // C) Expansión: Obtener acciones posibles y simular el futuro
            ArrayList<ACTIONS> accionesPosibles = estadoActual.getAvailableActions();
            for (ACTIONS accion : accionesPosibles) {
                
                // Excluimos la acción NIL (quedarse quieto) porque en un laberinto estático no nos ayuda
                if (accion == ACTIONS.ACTION_NIL) continue;

                // Aplicamos el Forward Model de GVGAI
                StateObservation estadoHijo = estadoActual.copy();
                estadoHijo.advance(accion);

                // Creamos el nodo hijo y lo metemos en la pila
                Nodo hijo = new Nodo(estadoHijo, actual, accion, actual.coste + 1);
                frontera.push(hijo);
            }
        }
        
        return null; // Si se vacía la pila y no hemos devuelto nada, es que no hay solución
    }

    /**
     * Genera un String único para cada estado basado en la posición y el inventario.
     * Ejemplo: "5_12_1" (X=5, Y=12, Llaves=1)
     */
    private String generarIdEstado(StateObservation estado) {
        tools.Vector2d pos = estado.getAvatarPosition();
        
        // Contamos cuántos recursos (llaves) tenemos en el inventario
        int numRecursos = 0;
        java.util.HashMap<Integer, Integer> inventario = estado.getAvatarResources();
        if (inventario != null) {
            for (Integer cantidad : inventario.values()) {
                numRecursos += cantidad;
            }
        }
        
        // Formateamos las coordenadas al bloque de la cuadrícula
        int x = (int) (pos.x / estado.getBlockSize());
        int y = (int) (pos.y / estado.getBlockSize());
        
        return x + "_" + y + "_" + numRecursos;
    }

    /**
     * Reconstruye el camino desde el nodo final hasta la raíz siguiendo a los padres.
     */
    private void construirPlan(Nodo nodoFinal) {
        Nodo actual = nodoFinal;
        // Apilamos las acciones (porque vamos del final al principio)
        Stack<ACTIONS> pilaAcciones = new Stack<>();
        
        while (actual.padre != null) {
            pilaAcciones.push(actual.accion);
            actual = actual.padre;
        }
        
        // Vaciamos la pila en nuestra lista de plan (para que queden en orden desde el inicio)
        while (!pilaAcciones.isEmpty()) {
            planDeAccion.add(pilaAcciones.pop());
        }
    }

    /**
     * Método principal que el motor de GVGAI llama en cada tick del juego.
     */
    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Si tenemos acciones en nuestro plan, sacamos la primera y la borramos de la lista
        if (planDeAccion != null && !planDeAccion.isEmpty()) {
            return planDeAccion.remove(0);
        }
        
        // Si nos quedamos sin plan, nos quedamos quietos
        return ACTIONS.ACTION_NIL; 
    }
}