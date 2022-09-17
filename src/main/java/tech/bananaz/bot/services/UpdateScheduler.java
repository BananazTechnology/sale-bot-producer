package tech.bananaz.bot.services;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.bananaz.bot.models.Contract;
import tech.bananaz.bot.models.ContractCollection;
import tech.bananaz.bot.utils.ContractBuilder;
import tech.bananaz.models.Sale;
import tech.bananaz.repositories.SaleConfigPagingRepository;
import tech.bananaz.repositories.EventPagingRepository;
import static java.util.Objects.nonNull;
import static tech.bananaz.utils.EncryptionUtils.decryptSale;
import static tech.bananaz.utils.StringUtils.nonEquals;

@Component
public class UpdateScheduler extends TimerTask {
	
	// Security
	@Value("${bot.encryptionKey}")
	private String key;
	
	@Autowired
	private SaleConfigPagingRepository configs;
	
	@Autowired
	private ContractCollection contracts;
	
	@Autowired
	private EventPagingRepository events;
	
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
			try {
				Iterable<Sale> allListingConfigs = this.configs.findAll();
				for(Sale conf : allListingConfigs) {
					try {
						// Must use decrypted values
						Sale decryptedConf = decryptSale(this.key, conf);
						
						List<String> updatedItems = new ArrayList<>();
						Contract cont = this.contracts.getContractById(conf.getId());
						// Update existing object in memory
						if(nonNull(cont)) {
							// Strings and Integers
							// Contract Address
							if(nonEquals(cont.getContractAddress(), decryptedConf.getContractAddress())) {
								updatedItems.add(String.format("contractAddress: %s->%s", cont.getContractAddress(), decryptedConf.getContractAddress()));
								cont.setContractAddress(decryptedConf.getContractAddress());
							}
							// Interval
							if(nonEquals(cont.getInterval(), decryptedConf.getInterval())) {
								updatedItems.add(String.format("interval: %s->%s", cont.getInterval(), decryptedConf.getInterval()));
								cont.setInterval(decryptedConf.getInterval());
							}
							// Rarity
							if(nonEquals(cont.getRarityEngine(), decryptedConf.getRarityEngine())) {
								updatedItems.add(String.format("rarityEngine: %s->%s", cont.getRarityEngine(), decryptedConf.getRarityEngine()));
								cont.setRarityEngine(decryptedConf.getRarityEngine());
							}
							
	
							// Booleans
							// Burn Watcher
							if(nonEquals(cont.isBurnWatcher(), decryptedConf.getBurnWatcher())) {
								updatedItems.add(String.format("burnWatcher: %s->%s", cont.isBurnWatcher(), decryptedConf.getBurnWatcher()));
								cont.setBurnWatcher(decryptedConf.getBurnWatcher());
							}
							// Mint Watcher
							if(nonEquals(cont.isMintWatcher(), decryptedConf.getMintWatcher())) {
								updatedItems.add(String.format("mintWatcher: %s->%s", cont.isMintWatcher(), decryptedConf.getMintWatcher()));
								cont.setMintWatcher(decryptedConf.getMintWatcher());
							}
							// Show Bundles
							if(nonEquals(cont.isShowBundles(), decryptedConf.getShowBundles())) {
								updatedItems.add(String.format("showBundles: %s->%s", cont.isShowBundles(), decryptedConf.getShowBundles()));
								cont.setShowBundles(decryptedConf.getShowBundles());
							}
							// Exclude OpenSea
							if(nonEquals(cont.isExcludeOpensea(), decryptedConf.getExcludeOpensea())) {
								updatedItems.add(String.format("excludeOpensea: %s->%s", cont.isExcludeOpensea(), decryptedConf.getExcludeOpensea()));
								cont.setExcludeOpensea(decryptedConf.getExcludeOpensea());
							}
							// Exclude Looksrare
							if(nonEquals(cont.isExcludeLooks(), decryptedConf.getExcludeLooksrare())) {
								updatedItems.add(String.format("excludeLooksrare: %s->%s", cont.isExcludeLooks(), decryptedConf.getExcludeLooksrare()));
								cont.setExcludeLooks(decryptedConf.getExcludeLooksrare());
							}
							// Active
							if(nonEquals(cont.isActive(), decryptedConf.getActive())) {
								updatedItems.add(String.format("active: %s->%s", cont.isActive(), decryptedConf.getActive()));
								cont.setActive(decryptedConf.getActive());
							}
							// Slug
							if(nonEquals(cont.isSlug(), decryptedConf.getIsSlug())) {
								updatedItems.add(String.format("isSlug: %s->%s", cont.isSlug(), "true"));
								cont.setSlug(decryptedConf.getIsSlug());
							}
							// If Solana or Polygon set slug
							if(decryptedConf.getSolanaOnOpensea() || decryptedConf.getPolygonOnOpensea()) {
								if(!cont.isSlug()) {
									updatedItems.add(String.format("isSlug: %s->%s", cont.isSlug(), "true"));
									cont.setSlug(true);
								}
							}
	
						} 
						// Add new contract
						else {
							LOGGER.debug("Object NOT found in memory, building new");
							try {
								// Build required components for each entry
								Contract watcher = new ContractBuilder().configProperties(decryptedConf, this.configs, this.events);
								// Start the watcher
								watcher.startSalesScheduler();
								// Add this to internal memory buffer
								this.contracts.addContract(watcher);
								updatedItems.add(String.format("new: %s", watcher));
							} catch (Exception e) {
								LOGGER.error("Failed starting config with id {}, exception {}", conf.getId(), e.getMessage());
							}
						}
						if(updatedItems.size() > 0) {
							if(nonNull(cont)) cont.setConfig(conf);
							LOGGER.info("Contract {} updated {}", conf.getId(), Arrays.toString(updatedItems.toArray()));
						}
					} catch(Exception ex) {
						LOGGER.error("Failed inital parsing on id {}, exception {}", conf.getId(), ex.getMessage());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Cleanup
		/// Remove contracts which are marked not active
		for(Contract c : this.contracts.getContracts()) {
			if(!c.isActive()) {
				LOGGER.info("Object was not active, removing: {}", c.toString());
				removeContract(c);
			}
		}
		/// Ensure all contratcs in memory exist in the DB
		for(Contract cont : contracts.getContracts()) {
			try {
				boolean existsInDB = this.configs.existsById(cont.getId());
				if(!existsInDB) {
					LOGGER.info("Object was not in the db, removing: {}", cont.toString());
					removeContract(cont);
					// A limitation of this modification
					// An active loop cannot be modified then continued to loop
					// Any extra contracts removed next time
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void removeContract(Contract c) {
		c.stopSalesScheduler();
		this.contracts.removeContract(c);
	}
}