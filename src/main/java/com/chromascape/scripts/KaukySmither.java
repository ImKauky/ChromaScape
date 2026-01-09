package com.chromascape.scripts;

import com.chromascape.base.BaseScript;
import com.chromascape.utils.actions.PointSelector;
import com.chromascape.utils.core.input.distribution.ClickDistribution;
import com.chromascape.utils.core.screen.topology.TemplateMatching;
import com.chromascape.utils.core.screen.window.ScreenManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Smiths items at varrock west bank.
 * The bank should be tagged YELLOW (255, 255, 0) and the anvil should be tagged CYAN (0, 255, 255).
 * The inventory must be set up so that all of the bars are used up in the smithing process.
 * Ideally, either an imcando hammer is used, otherwise the hammer must be in a locked inventory slot.
 * Left-click bank options must be set to "all".
 * 
 * This script could be improved further by adding detection for red clicks (and repeated attempts in the case of yellow clicks)
 * This would reduce the chance of an action failing due to lag or misclicks.
 * 
 * This script was based off of the DemoWineScript.
 */
public class KaukySmither extends BaseScript {

  private final Logger logger = LogManager.getLogger(this.getClass());

  private static final String bar = "/images/user/smithing/Iron_bar.png";
  private static final String barBank = "/images/user/smithing/Iron_bar_bank.png";
  private static final String bankAll = "/images/user/bank/bank_all.png";
  private static final String smithMenu = "/images/user/smithing/smith_menu.png";

  private static final String bankTag = "Yellow";
  private static final String anvilTag = "Cyan";

  private static final int MAX_ATTEMPTS = 15;

  /** Constructs a BaseScript. */
  public KaukySmither() {
    super();
  }

  /**
   * The core logic of the script.
   *
   * <p>This method is called repeatedly in a loop by {@link #run()} for the specified duration.
   * Subclasses must override this method to implement their specific bot behavior.
   *
   * <p>Note: This method is called synchronously on the running thread.
   */
  @Override
  protected void cycle() {

    logger.info("Starting cycle - clicking bank tag");
    clickTag(bankTag); // Click the bank tag to open bank

    // Wait for bank to open
    logger.info("Waiting for bank to open");
    while (!checkIfImageInGameView(bankAll, 0.07)) {
      waitRandomMillis(500, 700);
    }

    waitRandomMillis(300, 900); // Wait a bit after bank opens for humanization

    logger.info("Bank opened - depositing all items");
    clickImage(bankAll, "medium", 0.055); // Deposit all
    waitRandomMillis(650, 750);

    logger.info("Withdrawing bars from bank");
    clickImage(barBank, "fast", 0.07); // Take out bars
    waitRandomMillis(300, 600);

    logger.info("Closing bank interface");
    pressEscape(); // Exit bank UI
    waitRandomMillis(600, 800);

    logger.info("Clicking anvil tag");
    clickTag(anvilTag); // Click the anvil tag to smith

    // Wait for smithing interface to open
    logger.info("Waiting for smithing interface to open");
    while (!checkIfImageInGameView(smithMenu, 0.07)) {
      waitRandomMillis(500, 700);
    }

    waitRandomMillis(200, 800); // Wait a bit after smithing menu opens for humanization

    logger.info("Smithing interface opened - starting smithing");
    pressSpace(); // Press Space to start

    // Wait for bars to not be in last inventory slot
    logger.info("Waiting for smithing to complete");
    logger.info("Threshold: 0.03");
    while (checkIfImageInInvLastSlot(bar, 0.03)) {
      waitRandomMillis(500, 700);
    }

    logger.info("Smithing completed - cycle finished");
    waitRandomMillis(600, 1200); // Wait for a random time after smithing completes for humanization
  }

  /**
   * Simulates pressing the Escape key by sending the key press and release events to the client
   * keyboard controller.
   */
  private void pressEscape() {
    controller().keyboard().sendModifierKey(401, "esc");
    waitRandomMillis(80, 100);
    controller().keyboard().sendModifierKey(402, "esc");
  }

  /**
   * Simulates pressing the Space key by sending the key press and release events to the client
   * keyboard controller.
   */
  private void pressSpace() {
    controller().keyboard().sendModifierKey(401, "space");
    waitRandomMillis(300, 500);
    controller().keyboard().sendModifierKey(402, "space");
  }

  /**
   * Attempts to locate and click a tagged object of the specified color within the game view. 
   * It searches for contours of the given color, then clicks a randomly distributed point inside 
   * the contour bounding box, retrying up to a maximum number of attempts. Logs failures and 
   * stops the script if unable to click successfully.
   *
   * @param color the color tag to search for (e.g., "Yellow", "Cyan", "Purple")
   */
  private void clickTag(String color) {
    Point clickLocation = new Point();
    try {
      clickLocation =
          PointSelector.getRandomPointInColour(
              controller().zones().getGameView(), color, MAX_ATTEMPTS);
    } catch (Exception e) {
      logger.error("Failed while generating {} tag click location: {}", color, String.valueOf(e));
      stop();
    }

    if (clickLocation == null) {
      logger.error("clickTag click location is null for color: {}", color);
      stop();
    }

    try {
      controller().mouse().moveTo(clickLocation, "medium");
      controller().mouse().leftClick();
      logger.info("Clicked on {} tagged object at {}", color, clickLocation);
    } catch (Exception e) {
      logger.error(e.getMessage());
      stop();
    }
  }

  /**
   * Searches for the provided image template within the current game view, then clicks a random
   * point within the detected bounding box if the match exceeds the defined threshold.
   *
   * @param imagePath the BufferedImage template to locate and click within the game view
   * @param speed the speed that the mouse moves to click the image
   * @param threshold the openCV threshold to decide if a match exists
   */
  private void clickImage(String imagePath, String speed, double threshold) {
    try {
      BufferedImage gameView = controller().zones().getGameView();
      Point clickLocation = PointSelector.getRandomPointInImage(imagePath, gameView, threshold);

      if (clickLocation == null) {
        logger.error("clickImage click location is null");
        stop();
      }

      controller().mouse().moveTo(clickLocation, speed);
      controller().mouse().leftClick();
      logger.info("Clicked on image at {}", clickLocation);

    } catch (Exception e) {
      logger.error("clickImage failed: {}", e.getMessage());
      stop();
    }
  }

  /**
   * Checks if an image exists on the screen and returns a boolean referring to if it was detected.
   *
   * @param imagePath the path to the image being searched
   * @param threshold the openCV threshold to decide if a match exists
   * @return true if the image exists in the inventory slot 27, else false
   */
  private boolean checkIfImageInInvLastSlot(String imagePath, double threshold) {
    try {
      BufferedImage inventorySlot27 =
          ScreenManager.captureZone(controller().zones().getInventorySlots().get(27));
      Rectangle boundingBox = TemplateMatching.match(imagePath, inventorySlot27, threshold, false);
      if (boundingBox == null || boundingBox.isEmpty()) {
        logger.error("Template match failed: No valid inventory bounding box.");
        return false;
      }

      //logger.info("Template match succeeded: Image found in inventory slot 27.");

      return true;

    } catch (Exception e) {
      logger.error("checkIfImageInv failed: {}", e.getMessage());
      stop();
    }
    return false;
  }

  /**
   * Checks if an image exists in the game view and returns a boolean referring to if it was detected.
   *
   * @param imagePath the path to the image being searched
   * @param threshold the openCV threshold to decide if a match exists
   * @return true if the image exists in the game view, else false
   */
  private boolean checkIfImageInGameView(String imagePath, double threshold) {
    try {
      BufferedImage gameView = controller().zones().getGameView();
      Rectangle boundingBox = TemplateMatching.match(imagePath, gameView, threshold, false);
      if (boundingBox == null || boundingBox.isEmpty()) {
        logger.debug("Template match failed: Image not found in game view.");
        return false;
      }

      return true;

    } catch (Exception e) {
      logger.error("checkIfImageInGameView failed: {}", e.getMessage());
      stop();
    }
    return false;
  }
}
