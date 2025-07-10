// Blisterwood Chopper
// Author: rslider
// Version: 1.0
// Date: 2025-07-08
// Description: Chops Blisterwood trees and drops logs when inventory is full.
// Code inspired by OSMB's DaeyaltMiner, MotherloadMine, and AIOAnvil scripts, as well as Davyy's WC script.

package com.osmb.script.blisterwoodchopper;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.timing.Timer;
import java.util.Set;

@ScriptDefinition(name = "Blisterwood Chopper", description = "Chops Blisterwood trees and drops logs when inventory is full.", version = 1.0, author = "rslider", skillCategory = SkillCategory.WOODCUTTING)
public class BlisterwoodChopper extends Script {

    // TODO: Not 100% sure these are correct, but they work for now
    private static final WorldPosition BLISTERWOOD_TREE_POSITION = new WorldPosition(3635, 3362, 0);

    // TODO: Not 100% sure these are correct, but they work for now
    private static final Area CHOPPING_AREA = new RectangleArea(3632, 3360, 0, 5, 0);

    private static final int BLISTERWOOD_LOG_ID = ItemID.BLISTERWOOD_LOGS;

    private int choppingTimeout;
    private ItemGroupResult inventorySnapshot;

    public BlisterwoodChopper(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public void onStart() {
        this.choppingTimeout = RandomUtils.gaussianRandom(2500, 7500, 5000, 1200); // 2.5-7.5 seconds with 5s mean
        log(BlisterwoodChopper.class, "Starting Blisterwood Chopper...");
        log(BlisterwoodChopper.class, "Tree position: " + BLISTERWOOD_TREE_POSITION);
        log(BlisterwoodChopper.class, "Chopping area: " + CHOPPING_AREA);
        log(BlisterwoodChopper.class, "Looking for logs with ID: " + BLISTERWOOD_LOG_ID);
        log(BlisterwoodChopper.class, "Initial chopping timeout: " + choppingTimeout + "ms");
    }

    @Override
    public int poll() {
        // Update inventory snapshot
        inventorySnapshot = getWidgetManager().getInventory().search(Set.of(BLISTERWOOD_LOG_ID));

        // Close any open widgets
        if (ensureWidgetsCollapsed()) {
            return 0;
        }

        // Check if inventory is full and drop logs if needed
        if (inventorySnapshot.isFull()) {
            log(BlisterwoodChopper.class, "Inventory is full, dropping logs...");
            dropAllLogs();
            return RandomUtils.gaussianRandom(800, 2500, 1200, 400);
        }

        // Check if we're in the chopping area
        WorldPosition playerPosition = getWorldPosition();
        if (playerPosition != null && !CHOPPING_AREA.contains(playerPosition)) {
            log(BlisterwoodChopper.class, "Not in chopping area, walking to position...");
            getWalker().walkTo(CHOPPING_AREA.getRandomPosition());
            return RandomUtils.gaussianRandom(1200, 2800, 1800, 500);
        }

        // Find the blisterwood tree
        RSObject blisterwoodTree = getBlisterwoodTree();
        if (blisterwoodTree == null) {
            log(BlisterwoodChopper.class, "Blisterwood tree not found! Make sure you're near the tree.");
            return RandomUtils.gaussianRandom(2000, 4000, 2500, 600);
        }

        // Check if we're currently chopping
        // TODO: Might want to adjust minDifferenceFactor; not sure on best value
        if (getPixelAnalyzer().isPlayerAnimating(0.3)) {
            log(BlisterwoodChopper.class, "Currently chopping, waiting...");
            waitUntilFinishedChopping();
            return 0;
        }

        // We're not chopping, click the tree randomly within its bounds
        log(BlisterwoodChopper.class, "Clicking blisterwood tree...");
        if (clickTreeRandomly(blisterwoodTree)) {
            // Small delay after clicking before starting monitoring (like other scripts)
            int clickDelay = RandomUtils.gaussianRandom(800, 1500, 1000, 200);
            submitTask(() -> {
                waitUntilFinishedChopping();
                return false;
            }, clickDelay);
            return 0;
        }

        return RandomUtils.gaussianRandom(400, 1200, 600, 200);
    }

    private boolean ensureWidgetsCollapsed() {
        if (getWidgetManager().getChatbox().isOpen()) {
            getWidgetManager().getChatbox().close();
            return true;
        }
        if (!getWidgetManager().getTabManager().closeContainer()) {
            log(BlisterwoodChopper.class, "Failed to close tab container.");
            return true;
        }
        return false;
    }

    private void dropAllLogs() {
        // Count how many logs we have
        int logCount = inventorySnapshot.getAmount(BLISTERWOOD_LOG_ID);

        if (logCount > 0) {
            log(BlisterwoodChopper.class, "Dropping " + logCount + " logs");
            // Drop all logs at once
            getWidgetManager().getInventory().dropItem(BLISTERWOOD_LOG_ID, logCount);

            // Small delay after dropping
            int dropDelay = RandomUtils.gaussianRandom(400, 1500, 700, 300);
            log(BlisterwoodChopper.class, "Humanized delay after dropping: " + dropDelay + "ms");
            submitTask(() -> false, dropDelay);
        }
    }

    private RSObject getBlisterwoodTree() {
        // Use the simple approach like other scripts (Anvil, Furnace, etc.)
        RSObject tree = getObjectManager().getClosestObject("Blisterwood tree");
        if (tree != null) {
            log(BlisterwoodChopper.class, "Found blisterwood tree at: " + tree.getWorldPosition());
        } else {
            log(BlisterwoodChopper.class, "No blisterwood tree found nearby");
        }
        return tree;
    }

    private boolean clickTreeRandomly(RSObject tree) {
        // Get the tree's visual bounds (polygon)
        Polygon treePolygon = tree.getConvexHull();

        if (treePolygon != null) {
            // Get bounding rectangle of the polygon
            Rectangle bounds = treePolygon.getBounds();

            if (bounds != null) {
                // Apply 20% trim like OSMB/Davyy's approach for more human-like clicking
                int trimX = (int) (bounds.width * 0.2);
                int trimY = (int) (bounds.height * 0.2);

                // Ensure trimmed area is still valid
                int newWidth = bounds.width - (trimX * 2);
                int newHeight = bounds.height - (trimY * 2);

                if (newWidth > 0 && newHeight > 0) {
                    Rectangle trimmedBounds = new Rectangle(
                            bounds.x + trimX,
                            bounds.y + trimY,
                            newWidth,
                            newHeight);

                    log(BlisterwoodChopper.class, "Clicking in centre 60% of tree");
                    return getFinger().tap(trimmedBounds, "Chop");
                } else {
                    log(BlisterwoodChopper.class, "Trimmed bounds too small, using full polygon");
                    return getFinger().tap(treePolygon, "Chop");
                }
            } else {
                log(BlisterwoodChopper.class, "No bounding rectangle, using polygon");
                return getFinger().tap(treePolygon, "Chop");
            }
        } else {
            // Fall back to regular interact if no polygon is available
            log(BlisterwoodChopper.class, "No tree polygon found, using regular interact");
            return tree.interact("Chop");
        }
    }

    private void waitUntilFinishedChopping() {
        log(BlisterwoodChopper.class, "Monitoring chopping animation...");

        // Direct animation monitoring like DaeyaltMiner (no grace period complexity)
        Timer animatingTimer = new Timer();

        submitHumanTask(() -> {
            // If we haven't animated for the timeout period, we're done chopping
            if (animatingTimer.timeElapsed() > choppingTimeout) {
                log(BlisterwoodChopper.class, "Animation timeout - finished chopping");
                this.choppingTimeout = RandomUtils.gaussianRandom(2500, 7500, 5000, 1200);
                log(BlisterwoodChopper.class,
                        "Next chopping timeout: " + choppingTimeout + "ms (gaussian distributed)");
                return true;
            }

            // Reset timer every time we detect chopping animation (like mining scripts)
            // TODO: Might want to adjust minDifferenceFactor; not sure on best value
            if (getPixelAnalyzer().isPlayerAnimating(0.3)) {
                animatingTimer.reset(); // Keep resetting while actively chopping
            }

            return false; // Keep waiting
        }, RandomUtils.gaussianRandom(45000, 120000, 75000, 20000)); // Max wait time
    }

    @Override
    public int[] regionsToPrioritise() {
        // TODO: I need to get more familiar with the debug tool. I really don't know if
        // these values are correct.
        // Blisterwood tree regions (from debug: 14131, 14132, 14133, 14387, 14388,
        // 14389, 14643, 14644, 14645)
        return new int[] { 14388 }; // I legit just picked the middle value
    }
}