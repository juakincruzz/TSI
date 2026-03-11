package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import core.game.StateObservation;
import ontology.Types.ACTIONS;

public class Nodo {
    
    public StateObservation estado;
    public Nodo padre;
    public ACTIONS accion;
    public int coste;
    public int profundidad;
    
    // Constructor 1
    public Nodo(StateObservation estado) {
        this.estado = estado;
        this.padre = null;
        this.accion = ACTIONS.ACTION_NIL;
        this.coste = 0;
        this.profundidad = 0;
    }
    
    // Constructor 2
    public Nodo(StateObservation estado, Nodo padre, ACTIONS accion, int coste) {
        this.estado = estado;
        this.padre = padre;
        this.accion = accion;
        this.coste = coste;
        this.profundidad = padre.profundidad + 1;
    }
}