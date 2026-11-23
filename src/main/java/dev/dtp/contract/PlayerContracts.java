package dev.dtp.contract;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.dtp.data.Versioned;

/**
 * Contract-Daten am Spieler: angenommene Contracts und abgeschlossene Contracts je Stufe.
 * Überlebt den Tod — nur getragene Taler gehen verloren, nie der Auftragsfortschritt.
 *
 * Dazu die <b>persönliche Contract-Uhr</b> ({@link #ownDay()}): Sie läuft nur, während der Spieler online ist.
 * Angenommene Contracts sind auf diese Uhr gestempelt — wer nicht spielt, verliert keinen Contract-Wert,
 * auch wenn Mitspieler weiterspielen.
 */
public class PlayerContracts {
    /** Persönliche Uhr noch nicht gestartet — Spielstand von vor Version 2 oder neuer Spieler. */
    private static final double NOT_STARTED = -1.0;

    private static final Codec<PlayerContracts> INNER = RecordCodecBuilder.create(i -> i.group(
            Contract.CODEC.listOf().optionalFieldOf("accepted", List.of()).forGetter(d -> d.accepted),
            Codec.INT.listOf().optionalFieldOf("completed_per_tier", List.of()).forGetter(d -> d.completedPerTier),
            Codec.DOUBLE.optionalFieldOf("own_day", NOT_STARTED).forGetter(d -> d.ownDay)
    ).apply(i, PlayerContracts::new));

    /**
     * Version 2 führt die persönliche Contract-Uhr ein. Bestehende angenommene Contracts sind mit der serverweiten
     * Contract-Uhr gestempelt; die persönliche Uhr startet deshalb beim ersten Zugriff auf deren Stand
     * ({@link #ensureStarted}) — hier ist nichts umzurechnen.
     */
    private static final Versioned.Step V1_ADD_OWN_CLOCK = new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            return data;
        }
    };

    public static final Codec<PlayerContracts> CODEC = Versioned.codec(2, INNER, List.of(V1_ADD_OWN_CLOCK));

    private final List<Contract> accepted;
    /** Index 0 = Stufe 1. */
    private final List<Integer> completedPerTier;
    private double ownDay;

    public PlayerContracts() {
        this(List.of(), List.of(), NOT_STARTED);
    }

    private PlayerContracts(List<Contract> accepted, List<Integer> completedPerTier, double ownDay) {
        this.accepted = new ArrayList<>(accepted);
        this.completedPerTier = new ArrayList<>(completedPerTier);
        this.ownDay = ownDay;
    }

    /** Persönliche Contract-Uhr in Minecraft-Tagen: nur Zeit, in der dieser Spieler online war. */
    public double ownDay() {
        return ownDay;
    }

    public boolean started() {
        return ownDay >= 0.0;
    }

    /** Startet die Uhr auf dem Stand der serverweiten Contract-Uhr, falls sie noch nicht läuft. */
    public void ensureStarted(double contractDay) {
        if (!started()) {
            ownDay = contractDay;
        }
    }

    public void advance(double days) {
        if (started()) {
            ownDay += days;
        }
    }

    public List<Contract> accepted() {
        return accepted;
    }

    public int completed(int tier) {
        return tier - 1 < completedPerTier.size() ? completedPerTier.get(tier - 1) : 0;
    }

    public void addCompleted(int tier) {
        while (completedPerTier.size() < tier) {
            completedPerTier.add(0);
        }
        completedPerTier.set(tier - 1, completedPerTier.get(tier - 1) + 1);
    }
}
