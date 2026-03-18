package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import java.util.ArrayList;
import core.game.StateObservation;
import ontology.Types.ACTIONS;

/**
 * Nodo del árbol de búsqueda.
 * Usado tanto por AgenteProfundidad (DFS) como por AgenteAStar.
 *
 * Cada nodo almacena:
 *   - estado: StateObservation del juego en este punto
 *   - padre: nodo predecesor en el árbol
 *   - accion: primera acción que llevó del padre a este nodo
 *   - accionesDesdeParent: lista completa de acciones (para catapultas,
 *     una transición puede requerir múltiples ticks: pisar + NILs de vuelo)
 *   - coste: coste acumulado desde la raíz
 *   - profundidad: nivel en el árbol de búsqueda
 */
public class Nodo {

    public StateObservation estado;
    public Nodo padre;
    public ACTIONS accion;                    // primera acción (compatibilidad)
    public ArrayList<ACTIONS> accionesDesdeParent; // secuencia completa de acciones
    public int coste;
    public int profundidad;

    /**
     * Constructor para el nodo raíz.
     */
    public Nodo(StateObservation estado) {
        this.estado = estado;
        this.padre = null;
        this.accion = ACTIONS.ACTION_NIL;
        this.accionesDesdeParent = null;
        this.coste = 0;
        this.profundidad = 0;
    }

    /**
     * Constructor para nodos hijos con lista de acciones agrupada.
     * Usado cuando una transición requiere múltiples ticks (catapultas).
     */
    public Nodo(StateObservation estado, Nodo padre, ArrayList<ACTIONS> acciones, int coste) {
        this.estado = estado;
        this.padre = padre;
        this.accion = (acciones != null && !acciones.isEmpty()) ? acciones.get(0) : ACTIONS.ACTION_NIL;
        this.accionesDesdeParent = acciones;
        this.coste = coste;
        this.profundidad = padre.profundidad + 1;
    }

    /**
     * Constructor simple para nodos hijos con una sola acción.
     */
    public Nodo(StateObservation estado, Nodo padre, ACTIONS accion, int coste) {
        this.estado = estado;
        this.padre = padre;
        this.accion = accion;
        this.accionesDesdeParent = new ArrayList<>();
        this.accionesDesdeParent.add(accion);
        this.coste = coste;
        this.profundidad = padre.profundidad + 1;
    }
}