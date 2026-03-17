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
 *   FASE 1 – Búsqueda A* en el constructor.
 *            Las catapultas se agrupan como una transición:
 *              pisar (1 tick) + transformación (1 NIL) + vuelo (N NILs) + retransformación (1 NIL)
 *            Se detecta el fin del vuelo cuando avatarType vuelve al tipo original.
 *
 *   FASE 2 – act() devuelve acciones precalculadas.
 *
 * Mecánica de catapulta (confirmada por debug):
 *   ACTION_DOWN a (2,3): type cambia de 9→12, monedas bajan. Posición = catapulta.
 *   NIL#1:  type=12, pos sin cambio (transformación)
 *   NIL#2–10: type=12, pos avanza 1 celda/tick (vuelo)
 *   NIL#10: type=12, pos=(2,12), puedeMover=false (aterrizando, aún murciélago)
 *   NIL#11: type=9, pos=(2,12), puedeMover=true  ← CORTE CORRECTO
 */
public class AgenteAStar extends AbstractPlayer {

    private int blockSize;
    private int metaX, metaY;
    private ArrayList<ACTIONS> plan = new ArrayList<>();
    private int nodosExpandidos = 0, profMax = 0;

    // =========================================================
    //  CONSTRUCTOR — ejecuta A* completo
    // =========================================================
    public AgenteAStar(StateObservation so, ElapsedCpuTimer timer) {
        super();
        blockSize = so.getBlockSize();

        ArrayList<Observation>[] portales = so.getPortalsPositions();
        if (portales != null && portales.length > 0 && !portales[0].isEmpty()) {
            metaX = gridX(portales[0].get(0).position);
            metaY = gridY(portales[0].get(0).position);
        }

        System.out.println("AgenteAStar inicializado. Meta: (" + metaX + "," + metaY + ")");
        plan = buscarAStar(so);
    }

    // =========================================================
    //  ACT
    // =========================================================
    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (plan.isEmpty()) return ACTIONS.ACTION_NIL;
        return plan.remove(0);
    }

    // =========================================================
    //  FASE 1: BÚSQUEDA A*
    // =========================================================
    private ArrayList<ACTIONS> buscarAStar(StateObservation soInicial) {

        PriorityQueue<Nodo> abiertos = new PriorityQueue<>();
        HashMap<String, Nodo> abiertosMapa = new HashMap<>();
        HashMap<String, Nodo> cerrados = new HashMap<>();

        Nodo nodoInicial = new Nodo(soInicial, null, null, 0,
                                    heuristica(soInicial), 0);
        String k0 = stateKey(soInicial);
        abiertos.add(nodoInicial);
        abiertosMapa.put(k0, nodoInicial);

        Nodo meta = null;

        while (!abiertos.isEmpty()) {
            Nodo actual = abiertos.poll();

            // Lazy deletion
            if (actual.obsoleto) continue;
            String keyActual = stateKey(actual.so);
            Nodo enMapa = abiertosMapa.get(keyActual);
            if (enMapa != actual) continue;
            abiertosMapa.remove(keyActual);

            nodosExpandidos++;
            if (actual.prof > profMax) profMax = actual.prof;

            // ¿Victoria?
            if (actual.so.getGameWinner() == ontology.Types.WINNER.PLAYER_WINS) {
                meta = actual;
                break;
            }

            // Mover a cerrados
            cerrados.put(keyActual, actual);

            // Expandir
            int avatarTypeActual = actual.so.getAvatarType();
            int monedasActual = actual.so.getAvatarResources().getOrDefault(15, 0);
            Vector2d posActual = actual.so.getAvatarPosition();
            int gxAct = gridX(posActual), gyAct = gridY(posActual);

            ACTIONS[] dirs = {ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_LEFT,
                              ACTIONS.ACTION_UP,    ACTIONS.ACTION_DOWN};

            for (ACTIONS dir : dirs) {
                StateObservation copia = actual.so.copy();
                copia.advance(dir);

                // Muerto → descartar
                if (copia.isGameOver() &&
                    copia.getGameWinner() != ontology.Types.WINNER.PLAYER_WINS)
                    continue;

                ArrayList<ACTIONS> acciones = new ArrayList<>();
                acciones.add(dir);
                int coste = 1;

                // ¿Catapulta activada?
                // Detección: el tipo del avatar cambió (vampiro 9 → murciélago 12)
                // y las monedas bajaron
                if (!copia.isGameOver()) {
                    int monedasDespues = copia.getAvatarResources().getOrDefault(15, 0);
                    int avatarTypeDespues = copia.getAvatarType();

                    boolean catapulta = (avatarTypeDespues != avatarTypeActual)
                        && (monedasDespues < monedasActual);

                    if (catapulta) {
                        // Simular vuelo con NILs hasta que el avatar
                        // vuelva a su tipo original (retransformación completa)
                        for (int t = 0; t < 30; t++) {
                            copia.advance(ACTIONS.ACTION_NIL);
                            acciones.add(ACTIONS.ACTION_NIL);
                            coste++;

                            if (copia.isGameOver()) break;

                            // Corte: el avatar volvió a su tipo original
                            if (copia.getAvatarType() == avatarTypeActual) {
                                break;
                            }
                        }
                    }
                }

                // Victoria
                if (copia.getGameWinner() == ontology.Types.WINNER.PLAYER_WINS) {
                    meta = new Nodo(copia, actual, acciones,
                                    actual.g + coste, 0, actual.prof + 1);
                    break;
                }

                // Muerto tras vuelo → descartar
                if (copia.isGameOver()) continue;

                String keySuc = stateKey(copia);
                int gNuevo = actual.g + coste;

                // Caso 1: en cerrados con mejor g → reabrir
                Nodo enCerr = cerrados.get(keySuc);
                if (enCerr != null) {
                    if (gNuevo < enCerr.g) {
                        cerrados.remove(keySuc);
                        Nodo ns = new Nodo(copia, actual, acciones, gNuevo,
                                           heuristica(copia), actual.prof + 1);
                        abiertos.add(ns);
                        abiertosMapa.put(keySuc, ns);
                    }
                    continue;
                }

                // Caso 2: no en abiertos ni cerrados → añadir
                Nodo enAb = abiertosMapa.get(keySuc);
                if (enAb == null) {
                    Nodo ns = new Nodo(copia, actual, acciones, gNuevo,
                                       heuristica(copia), actual.prof + 1);
                    abiertos.add(ns);
                    abiertosMapa.put(keySuc, ns);
                }
                // Caso 3: en abiertos con peor g → actualizar
                else if (gNuevo < enAb.g) {
                    enAb.obsoleto = true;
                    Nodo ns = new Nodo(copia, actual, acciones, gNuevo,
                                       heuristica(copia), actual.prof + 1);
                    abiertos.add(ns);
                    abiertosMapa.put(keySuc, ns);
                }
            }
            if (meta != null) break;
        }

        // ── FASE 2: Decodificar nodos → acciones ──
        ArrayList<ACTIONS> resultado = new ArrayList<>();
        if (meta != null) {
            Deque<Nodo> pila = new ArrayDeque<>();
            for (Nodo n = meta; n.padre != null; n = n.padre) pila.push(n);
            while (!pila.isEmpty()) {
                Nodo n = pila.pop();
                if (n.accionesDesdeParent != null)
                    resultado.addAll(n.accionesDesdeParent);
            }
            System.out.println("Plan encontrado: " + resultado.size() + " acciones.");
        } else {
            System.out.println("No se encontró solución.");
        }

        // Métricas
        MetricsProvider mp = MetricsProvider.getInstance();
        mp.setNodosExpandidos(nodosExpandidos);
        mp.setProfundidadMaxima(profMax);
        mp.setNodosAbiertos(abiertosMapa.size());
        mp.setNodosCerrados(cerrados.size());
        mp.setNumAccionesPlan(meta != null ? resultado.size() : -1);
        mp.printMetrics();

        return resultado;
    }

    // =========================================================
    //  HEURÍSTICA
    // =========================================================
    private double heuristica(StateObservation so) {
        Vector2d pos = so.getAvatarPosition();
        int gx = gridX(pos), gy = gridY(pos);

        ArrayList<Observation>[] rec = so.getResourcesPositions();
        if (rec != null) {
            for (ArrayList<Observation> lista : rec) {
                for (Observation obs : lista) {
                    if (obs.itype == 16) {
                        int lx = gridX(obs.position);
                        int ly = gridY(obs.position);
                        return Math.abs(gx - lx) + Math.abs(gy - ly)
                             + Math.abs(lx - metaX) + Math.abs(ly - metaY);
                    }
                }
            }
        }
        return Math.abs(gx - metaX) + Math.abs(gy - metaY);
    }

    // =========================================================
    //  CLAVE DE ESTADO
    // =========================================================
    private String stateKey(StateObservation so) {
        Vector2d pos = so.getAvatarPosition();
        int gx = gridX(pos), gy = gridY(pos);
        int monedas = so.getAvatarResources().getOrDefault(15, 0);

        long resBits = 0;
        ArrayList<Observation>[] rec = so.getResourcesPositions();
        if (rec != null) {
            for (ArrayList<Observation> lista : rec) {
                for (Observation obs : lista) {
                    int rx = gridX(obs.position);
                    int ry = gridY(obs.position);
                    resBits |= (1L << ((ry * 16 + rx) & 63));
                }
            }
        }
        return gx + "," + gy + "," + monedas + "," + resBits;
    }

    // =========================================================
    //  UTILIDADES
    // =========================================================
    private int gridX(Vector2d pos) { return (int)(pos.x / blockSize); }
    private int gridY(Vector2d pos) { return (int)(pos.y / blockSize); }

    // =========================================================
    //  NODO A*
    // =========================================================
    private static class Nodo implements Comparable<Nodo> {
        StateObservation so;
        Nodo padre;
        ArrayList<ACTIONS> accionesDesdeParent;
        int g, prof;
        double h;
        boolean obsoleto = false;

        Nodo(StateObservation so, Nodo padre, ArrayList<ACTIONS> acciones,
             int g, double h, int prof) {
            this.so = so;
            this.padre = padre;
            this.accionesDesdeParent = acciones;
            this.g = g;
            this.h = h;
            this.prof = prof;
        }

        double f() { return g + h; }

        @Override
        public int compareTo(Nodo o) {
            double f1 = f(), f2 = o.f();
            if (f1 != f2) return Double.compare(f1, f2);
            return Double.compare(h, o.h);
        }
    }
}