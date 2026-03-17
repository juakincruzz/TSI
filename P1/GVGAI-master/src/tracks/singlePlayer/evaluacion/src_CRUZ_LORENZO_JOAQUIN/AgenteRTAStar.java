package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tracks.singlePlayer.MetricsProvider;

// Nota: Se puede heredar de otras clases personalizadas por el alumnado que hereden de AbstractPlayer para generalizar
// los elementos comunes a todos los algoritmos.

public class AgenteRTAStar extends AbstractPlayer {

    public AgenteRTAStar(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        super();

        // El constructor puede inicializar todas las estructuras iniciales y hasta el nodo inicial pero no puede hacer 
        // nada del proceso de búsqueda
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Este método debe devolver la siguiente acción sin calcular un plan completo

        // Cuando el siguiente paso sea el objetivo o una muerte segura se deben fijar TODAS las métricas complementarias correspondientes al algoritmo
        MetricsProvider.getInstance().setNumAccionesPlan(-1);

        // ....
        // ....

        
        // Puede mostrarlas por pantalla, si lo desea, de la siguiete manera
        MetricsProvider.getInstance().printMetrics();

        return ACTIONS.ACTION_NIL;
    }
    
}
