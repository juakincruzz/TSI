package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import java.util.*;
import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tracks.singlePlayer.MetricsProvider;

/**
 * Agente A* — Práctica 1 TSI (UGR 2025-26)
 *
 * Búsqueda heurística offline con advance() del motor.
 * Cada tick = 1 nodo, coste 1. Sin agrupamiento de catapultas.
 * Heurística: Manhattan al portal.
 * Desempate: menor h(n), luego FIFO.
 * Orden expansión: R, U, L, D, NIL.
 * Búsqueda asíncrona en act() repartida entre ticks.
 */
public class AgenteAStar extends AbstractPlayer {

    private int blockSize;
    private int metaX, metaY;
    private int avatarTypeNormal;

    // Estructuras persistentes entre ticks
    private PriorityQueue<NodoAStar> abiertos;
    private HashMap<String, NodoAStar> abiertosMapa;
    private HashMap<String, NodoAStar> cerrados;
    private int ordenInsercion;

    private ArrayList<ACTIONS> plan;
    private boolean buscando;
    private boolean metricasEnviadas;
    private int nodosExpandidos, profMax;

    private static final ACTIONS[] ORDEN_EXPANSION = {
        ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT,  ACTIONS.ACTION_DOWN,
        ACTIONS.ACTION_NIL
    };

    // =========================================================
    //  CONSTRUCTOR — inicialización + nodo raíz
    // =========================================================
    public AgenteAStar(StateObservation so, ElapsedCpuTimer timer) {
        super();
        blockSize = so.getBlockSize();
        avatarTypeNormal = so.getAvatarType();

        ArrayList<Observation>[] portales = so.getPortalsPositions();
        if (portales != null && portales.length > 0 && !portales[0].isEmpty()) {
            metaX = gridX(portales[0].get(0).position);
            metaY = gridY(portales[0].get(0).position);
        }

        // Inicializar estructuras
        abiertos = new PriorityQueue<>();
        abiertosMapa = new HashMap<>();
        cerrados = new HashMap<>();
        ordenInsercion = 0;
        plan = null;
        buscando = true;
        metricasEnviadas = false;
        nodosExpandidos = 0;
        profMax = 0;

        // Nodo raíz
        NodoAStar n0 = new NodoAStar(so, null, ACTIONS.ACTION_NIL,
            0, heuristica(so), 0, ordenInsercion++);
        String k0 = stateKey(so);
        abiertos.add(n0);
        abiertosMapa.put(k0, n0);
    }

    // =========================================================
    //  ACT
    // =========================================================
    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (buscando) {
            buscarAStar(timer);
        }
        if (!buscando && plan != null && !plan.isEmpty()) {
            return plan.remove(0);
        }
        return ACTIONS.ACTION_NIL;
    }

    // =========================================================
    //  A* ASÍNCRONO
    // =========================================================
    private void buscarAStar(ElapsedCpuTimer timer) {
        NodoAStar meta = null;

        while (!abiertos.isEmpty()) {
            if (timer.remainingTimeMillis() < 3) return;

            NodoAStar actual = abiertos.poll();
            if (actual.obsoleto) continue;
            String keyActual = stateKey(actual.so);
            NodoAStar enMapa = abiertosMapa.get(keyActual);
            if (enMapa != actual) continue;
            abiertosMapa.remove(keyActual);

            nodosExpandidos++;
            if (actual.prof > profMax) profMax = actual.prof;

            if (actual.so.getGameWinner() == ontology.Types.WINNER.PLAYER_WINS) {
                meta = actual; break;
            }

            cerrados.put(keyActual, actual);

            for (ACTIONS accion : ORDEN_EXPANSION) {
                StateObservation copia = actual.so.copy();
                copia.advance(accion);

                if (copia.isGameOver() &&
                    copia.getGameWinner() != ontology.Types.WINNER.PLAYER_WINS)
                    continue;

                int gNuevo = actual.g + 1;
                double hNuevo = heuristica(copia);

                if (copia.getGameWinner() == ontology.Types.WINNER.PLAYER_WINS) {
                    meta = new NodoAStar(copia, actual, accion,
                        gNuevo, 0, actual.prof + 1, ordenInsercion++);
                    break;
                }

                String keySuc = stateKey(copia);

                NodoAStar enCerr = cerrados.get(keySuc);
                if (enCerr != null) {
                    if (gNuevo < enCerr.g) {
                        cerrados.remove(keySuc);
                        NodoAStar ns = new NodoAStar(copia, actual, accion,
                            gNuevo, hNuevo, actual.prof + 1, ordenInsercion++);
                        abiertos.add(ns);
                        abiertosMapa.put(keySuc, ns);
                    }
                    continue;
                }

                NodoAStar enAb = abiertosMapa.get(keySuc);
                if (enAb == null) {
                    NodoAStar ns = new NodoAStar(copia, actual, accion,
                        gNuevo, hNuevo, actual.prof + 1, ordenInsercion++);
                    abiertos.add(ns);
                    abiertosMapa.put(keySuc, ns);
                } else if (gNuevo < enAb.g) {
                    enAb.obsoleto = true;
                    NodoAStar ns = new NodoAStar(copia, actual, accion,
                        gNuevo, hNuevo, actual.prof + 1, ordenInsercion++);
                    abiertos.add(ns);
                    abiertosMapa.put(keySuc, ns);
                }
            }
            if (meta != null) break;
        }

        if (meta != null || abiertos.isEmpty()) {
            buscando = false;
            plan = new ArrayList<>();
            if (meta != null) {
                Deque<ACTIONS> pila = new ArrayDeque<>();
                for (NodoAStar n = meta; n.padre != null; n = n.padre)
                    pila.push(n.accion);
                while (!pila.isEmpty()) plan.add(pila.pop());
            }
            if (!metricasEnviadas) {
                MetricsProvider mp = MetricsProvider.getInstance();
                mp.setNodosExpandidos(nodosExpandidos);
                mp.setProfundidadMaxima(profMax);
                mp.setNodosAbiertos(abiertosMapa.size());
                mp.setNodosCerrados(cerrados.size());
                mp.setNumAccionesPlan(meta != null ? plan.size() : -1);
                mp.printMetrics();
                metricasEnviadas = true;
            }
        }
    }

    // =========================================================
    //  HEURÍSTICA — Manhattan al portal (obligatoria)
    // =========================================================
    private double heuristica(StateObservation so) {
        Vector2d pos = so.getAvatarPosition();
        return Math.abs(gridX(pos) - metaX) + Math.abs(gridY(pos) - metaY);
    }

    // =========================================================
    //  CLAVE DE ESTADO
    //  Cuando type != normal → añadir gameTick para distinguir
    //  ticks de vuelo con misma posición.
    // =========================================================
    private String stateKey(StateObservation so) {
        Vector2d pos = so.getAvatarPosition();
        int gx = gridX(pos), gy = gridY(pos);
        int monedas = so.getAvatarResources().getOrDefault(15, 0);
        int tipo = so.getAvatarType();

        // Recursos restantes en el mapa
        long resBits = 0;
        ArrayList<Observation>[] rec = so.getResourcesPositions();
        if (rec != null) {
            for (ArrayList<Observation> lista : rec)
                for (Observation obs : lista) {
                    int rx = gridX(obs.position), ry = gridY(obs.position);
                    resBits |= (1L << ((ry * 16 + rx) & 63));
                }
        }

        // Catapultas restantes (itype=5 en inmovables)
        long catBits = 0;
        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        if (inmov != null) {
            for (ArrayList<Observation> lista : inmov)
                for (Observation obs : lista)
                    if (obs.itype == 5) {
                        int cx = gridX(obs.position), cy = gridY(obs.position);
                        catBits |= (1L << ((cy * 16 + cx) & 63));
                    }
        }

        // Tipo no-normal → añadir tick para distinguir fases de vuelo
        if (tipo != avatarTypeNormal) {
            return gx + "," + gy + "," + monedas + "," + tipo + ","
                + resBits + "," + catBits + ",t" + so.getGameTick();
        }

        return gx + "," + gy + "," + monedas + "," + tipo + ","
            + resBits + "," + catBits;
    }

    private int gridX(Vector2d pos) { return (int)(pos.x / blockSize); }
    private int gridY(Vector2d pos) { return (int)(pos.y / blockSize); }

    // =========================================================
    //  NODO A*
    // =========================================================
    private static class NodoAStar implements Comparable<NodoAStar> {
        StateObservation so;
        NodoAStar padre;
        ACTIONS accion;
        int g, prof, orden;
        double h;
        boolean obsoleto = false;

        NodoAStar(StateObservation so, NodoAStar padre, ACTIONS accion,
                  int g, double h, int prof, int orden) {
            this.so = so; this.padre = padre; this.accion = accion;
            this.g = g; this.h = h; this.prof = prof; this.orden = orden;
        }
        double f() { return g + h; }
        @Override
        public int compareTo(NodoAStar o) {
            double f1 = f(), f2 = o.f();
            if (f1 != f2) return Double.compare(f1, f2);
            if (h != o.h) return Double.compare(h, o.h);
            return Integer.compare(orden, o.orden);
        }
    }
}