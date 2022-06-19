package org.mobarena.placeholders;

import com.garbagemule.MobArena.MonsterManager;
import com.garbagemule.MobArena.framework.Arena;
import com.garbagemule.MobArena.util.timer.AutoStartTimer;
import com.garbagemule.MobArena.waves.WaveManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.function.Function;

class ArenaResolver {

    private final ArenaLookup lookup;
    private final ConfigurationSection config;

    ArenaResolver(ArenaLookup lookup, ConfigurationSection config) {
        this.lookup = lookup;
        this.config = config;
    }

    String resolve(OfflinePlayer target, String rest) {
        // mobarena_arena is invalid
        if (rest == null) {
            return null;
        }

        // mobarena_arena_<key> is invalid
        String[] parts = rest.split("_", 2);
        if (parts.length != 2) {
            return null;
        }

        String key = parts[0];
        String tail = parts[1];

        switch (tail) {
            case "slug": {
                return withArena(target, key, Arena::getSlug);
            }
            case "name": {
                return withArena(target, key, Arena::configName);
            }
            case "state": {
                return withArena(target, key, arena -> {
                    if (arena.inEditMode()) {
                        return config.getString("editing");
                    }
                    if (arena.isRunning()) {
                        return config.getString("running");
                    }
                    if (arena.isEnabled()) {
                        return config.getString("available");
                    }
                    if (!arena.isEnabled()) {
                        return config.getString("disabled");
                    }
                    return "";
                });
            }
            case "min-players": {
                return withNumber(target, key, Arena::getMinPlayers);
            }
            case "max-players": {
                return withNumber(target, key, Arena::getMaxPlayers);
            }
            case "auto-start-timer": {
                return withNumber(target, key, arena -> {
                    AutoStartTimer timer = arena.getAutoStartTimer();
                    return timer.getRemaining() / 20;
                });
            }
            case "current-wave": {
                return withNumber(target, key, arena -> {
                    WaveManager waves = arena.getWaveManager();
                    return waves.getWaveNumber();
                });
            }
            case "final-wave": {
                return withArena(target, key, arena -> {
                    WaveManager waves = arena.getWaveManager();
                    int value = waves.getFinalWave();
                    return (value > 0) ? String.valueOf(value) : "∞";
                });
            }
            case "lobby-players": {
                return withPlayerCount(target, key, Arena::getPlayersInLobby);
            }
            case "ready-players": {
                return withPlayerCount(target, key, Arena::getReadyPlayersInLobby);
            }
            case "live-players": {
                return withPlayerCount(target, key, Arena::getPlayersInArena);
            }
            case "dead-players": {
                return withNumber(target, key, arena -> {
                    if (!arena.isRunning()) {
                        return 0;
                    }
                    int total = arena.getPlayerCount();
                    int live = arena.getPlayersInArena().size();
                    return (total - live);
                });
            }
            case "initial-players": {
                return withNumber(target, key, arena -> {
                    if (!arena.isRunning()) {
                        return 0;
                    }
                    return arena.getPlayerCount();
                });
            }
            case "live-mobs": {
                return withNumber(target, key, arena -> {
                    MonsterManager monsters = arena.getMonsterManager();
                    return monsters.getMonsters().size();
                });
            }
            default: {
                return null;
            }
        }
    }

    private String withNumber(
        OfflinePlayer target,
        String key,
        Function<Arena, Number> resolver
    ) {
        return withArena(target, key, arena -> {
            Number value = resolver.apply(arena);
            return String.valueOf(value);
        });
    }

    private String withPlayerCount(
        OfflinePlayer target,
        String key,
        Function<Arena, Collection<Player>> resolver
    ) {
        return withArena(target, key, arena -> {
            Collection<Player> players = resolver.apply(arena);
            return String.valueOf(players.size());
        });
    }

    private String withArena(
        OfflinePlayer target,
        String key,
        Function<Arena, String> resolver
    ) {
        Arena arena = lookup.lookup(target, key);
        if (arena == null) {
            return "";
        }
        return resolver.apply(arena);
    }

}
