package tech.bananaz.bot.services;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.bananaz.bot.models.Contract;
import tech.bananaz.bot.models.ContractCollection;
import tech.bananaz.bot.models.SaleConfig;
import tech.bananaz.bot.models.SalesProperties;
import tech.bananaz.bot.repositories.SaleConfigRepository;
import tech.bananaz.bot.repositories.SaleEventRepository;
import tech.bananaz.bot.utils.RarityEngine;
import static java.util.Objects.nonNull;
import static tech.bananaz.bot.utils.StringUtils.nonEquals;

@Component
public class UpdateScheduler extends TimerTask {
	
	@Autowired
	private SaleConfigRepository configs;
	
	@Autowired
	private ContractCollection contracts;
	
	@Autowired
	private SaleEventRepository events;
	
	/** Important variables needed for Runtime */
	private final int REFRESH_REQ = 60000;
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateScheduler.class);
	private Timer timer = new Timer(); // creating timer
    private TimerTask task; // creating timer task
    private boolean active = false;
	
	public boolean start() {
		if(nonNull(this.contracts)) {
			this.active = true;
			this.task   = this;
			LOGGER.info(String.format("Starting new UpdateScheduler"));
			// Starts this new timer, starts at random time and runs per <interval> milliseconds
			this.timer.schedule(task, 1, REFRESH_REQ);
		}
		return active;
	}
	
	public boolean stop() {
		this.active = false;
		LOGGER.info("Stopping UpdateScheduler");
		return active;
	}

	@Override
	public void run() {
		if(nonNull(this.contracts) && active) {
			List<SaleConfig> allListingConfigs = this.configs.findAll();
			for(SaleConfig conf : allListingConfigs) {
				try {
					List<String> updatedItems = new ArrayList<>();
					Contract cont = this.contracts.getContractById(conf.getId());
					// Update existing object in memory
					if(nonNull(cont)) {
						// Strings and Integers
						// Contract Address
						if(nonEquals(cont.getContractAddress(), conf.getContractAddress())) {
							updatedItems.add(String.format("contractAddress: %s->%s", cont.getContractAddress(), conf.getContractAddress()));
							cont.setContractAddress(conf.getContractAddress());
						}
						// Interval
						if(nonEquals(cont.getInterval(), conf.getInterval())) {
							updatedItems.add(String.format("interval: %s->%s", cont.getInterval(), conf.getInterval()));
							cont.setInterval(conf.getInterval());
						}
						// Rarity Slug
						if(nonEquals(cont.getRaritySlug(), conf.getRaritySlugOverwrite())) {
							updatedItems.add(String.format("raritySlug: %s->%s", cont.getRaritySlug(), conf.getRaritySlugOverwrite()));
							cont.setRaritySlug(conf.getRaritySlugOverwrite());
						}
						// Rarity Engine
						RarityEngine rarityEngine = (conf.getRarityEngine() != null) ? RarityEngine.fromString(conf.getRarityEngine()): RarityEngine.RARITY_TOOLS;
						if(nonEquals(cont.getEngine().toString(), rarityEngine)) {
							updatedItems.add(String.format("raritySlug: %s->%s", cont.getEngine().toString(), conf.getAutoRarity()));
							cont.setEngine(rarityEngine);
						}
						

						// Booleans
						// Burn Watcher
						if(nonEquals(cont.isBurnWatcher(), conf.getBurnWatcher())) {
							updatedItems.add(String.format("burnWatcher: %s->%s", cont.isBurnWatcher(), conf.getBurnWatcher()));
							cont.setBurnWatcher(conf.getBurnWatcher());
						}
						// Mint Watcher
						if(nonEquals(cont.isMintWatcher(), conf.getMintWatcher())) {
							updatedItems.add(String.format("mintWatcher: %s->%s", cont.isMintWatcher(), conf.getMintWatcher()));
							cont.setMintWatcher(conf.getMintWatcher());
						}
						// Auto Rarity
						if(nonEquals(cont.isAutoRarity(), conf.getAutoRarity())) {
							updatedItems.add(String.format("autoRarity: %s->%s", cont.isAutoRarity(), conf.getAutoRarity()));
							cont.setAutoRarity(conf.getAutoRarity());
						}
						// Show Bundles
						if(nonEquals(cont.isShowBundles(), conf.getShowBundles())) {
							updatedItems.add(String.format("showBundles: %s->%s", cont.isShowBundles(), conf.getShowBundles()));
							cont.setShowBundles(conf.getShowBundles());
						}
						// Exclude OpenSea
						if(nonEquals(cont.isExcludeOpensea(), conf.getExcludeOpensea())) {
							updatedItems.add(String.format("excludeOpensea: %s->%s", cont.isExcludeOpensea(), conf.getExcludeOpensea()));
							cont.setExcludeOpensea(conf.getExcludeOpensea());
						}
						// Exclude Looksrare
						if(nonEquals(cont.isExcludeLooks(), conf.getExcludeLooksrare())) {
							updatedItems.add(String.format("excludeLooksrare: %s->%s", cont.isExcludeLooks(), conf.getExcludeLooksrare()));
							cont.setExcludeLooks(conf.getExcludeLooksrare());
						}
						// Active
						if(nonEquals(cont.isActive(), conf.getActive())) {
							updatedItems.add(String.format("active: %s->%s", cont.isActive(), conf.getActive()));
							cont.setActive(conf.getActive());
						}

					} 
					// Add new contract
					else {
						LOGGER.debug("Object NOT found in memory, building new");
						// Build required components for each entry
						Contract watcher = new SalesProperties().configProperties(conf, this.configs, this.events);
						// Start the watcher
						watcher.startSalesScheduler();
						// Add this to internal memory buffer
						this.contracts.addContract(watcher);
						updatedItems.add(String.format("new: %s", watcher));
					}
					if(updatedItems.size() > 0) LOGGER.debug("Contract {} updated {}", conf.getId(), Arrays.toString(updatedItems.toArray()));
				} catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		// Cleanup
		for(Contract c : this.contracts.getContracts()) {
			if(!c.isActive()) {
				LOGGER.debug("Object was found to not be active, removing: {}", c.toString());
				c.stopSalesScheduler();
				this.contracts.removeContract(c);
			}
		}
	}
}