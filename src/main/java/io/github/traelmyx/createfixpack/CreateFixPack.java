package io.github.traelmyx.createfixpack;

import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Fixes for long-standing Create bugs, each with its own switch. A fix whose bug Create has fixed itself switches off on
 * that Create version and later, so the pack never patches over the real fix.
 */
@Mod(CreateFixPack.MOD_ID)
public class CreateFixPack {
	public static final String MOD_ID = "create_fix_pack";
	public static final Logger LOGGER = LoggerFactory.getLogger("Create: Fix Pack");

	/** Every fix, in config file order. */
	public static final Map<String, Fix> FIXES = new LinkedHashMap<>();

	public static final Fix CRAFTER_KEEPS_OUTPUT = fix("crafterKeepsOutput", "4510", null,
		"Mechanical crafters waiting to put their result into a full inventory no longer drop it on the ground when the world loads.");
	public static final Fix GAUGE_NO_OVER_REQUEST = fix("gaugeNoOverRequest", "9987", null,
		"Factory gauges no longer order more than their target because the packager counted items as arrived when some were taken out.");
	public static final Fix LINK_KEEPS_WAITING_LINKS = experimental(fix("linkKeepsWaitingLinks", "5009", null,
		"Experimental, the bug itself is not reproduced by a test yet. Redstone links in a chunk that is briefly not fully loaded are no longer dropped from their frequency for good. Turn on if receivers stop following their transmitter after chunks reload."));
	public static final Fix GAUGE_SINGLEPLAYER_ONCE = experimental(fix("gaugeSingleplayerCountOnce", "9987", null,
		"Experimental, not covered by a test. In singleplayer, arrivals at a packager are counted once instead of twice. Turn on if gauges in singleplayer clear promises twice as fast as items arrive."));
	public static final Fix GAUGE_FORGET_WHILE_BLIND = experimental(fix("gaugeForgetStockWhileBlind", "9987", null,
		"Experimental, not covered by a test. When a packager briefly can't see its inventory, it forgets its stock record instead of remembering it as empty, so the next look doesn't count everything as new. Turn on if a gauge reorders everything after its inventory block was updated."));

	public static final class Fix {
		public final String key, issue, description;
		/** First Create version that fixes the bug itself, or null while it is still open upstream. */
		public final String fixedUpstreamIn;
		public volatile boolean enabled = true;
		volatile boolean fixedUpstream;

		Fix(String key, String issue, String fixedUpstreamIn, String description) {
			this.key = key;
			this.issue = issue;
			this.fixedUpstreamIn = fixedUpstreamIn;
			this.description = description;
		}

		public boolean on() {
			return enabled && !fixedUpstream;
		}
	}

	/** Fixes without a passing test start switched off. */
	private static Fix experimental(Fix fix) {
		fix.enabled = false;
		return fix;
	}

	private static Fix fix(String key, String issue, String fixedUpstreamIn, String description) {
		Fix fix = new Fix(key, issue, fixedUpstreamIn, description);
		FIXES.put(key, fix);
		return fix;
	}

	public CreateFixPack() {
		loadConfig(FMLPaths.CONFIGDIR.get());
		ArtifactVersion create = ModList.get().getModContainerById("create").map(c -> c.getModInfo().getVersion()).orElse(null);
		for (Fix fix : FIXES.values()) {
			if (create != null && fix.fixedUpstreamIn != null && create.compareTo(new DefaultArtifactVersion(fix.fixedUpstreamIn)) >= 0) {
				fix.fixedUpstream = true;
				LOGGER.info("Create {} fixes #{} itself; {} is off", create, fix.issue, fix.key);
			}
		}
	}

	static void loadConfig(Path configDir) {
		Path file = configDir.resolve(MOD_ID + ".properties");
		Properties props = new Properties();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				props.load(reader);
			} catch (IOException e) {
				LOGGER.warn("Could not read {}, using defaults", file, e);
			}
		}
		StringBuilder out = new StringBuilder("# Create: Fix Pack. true = fixed, false = Create's own behaviour.\n# Fixes switch themselves off on Create versions that fix the bug.\n");
		for (Fix fix : FIXES.values()) {
			String value = props.getProperty(fix.key);
			if (value != null) fix.enabled = Boolean.parseBoolean(value.trim());
			out.append("\n# Create #").append(fix.issue).append(": ").append(fix.description).append('\n')
				.append(fix.key).append('=').append(fix.enabled).append('\n');
		}
		try {
			Files.createDirectories(configDir);
			try (Writer writer = Files.newBufferedWriter(file)) {
				writer.write(out.toString());
			}
		} catch (IOException e) {
			LOGGER.warn("Could not write {}", file, e);
		}
	}
}
