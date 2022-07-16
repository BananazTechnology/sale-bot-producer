package tech.bananaz.bot.services;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.Getter;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import tech.bananaz.bot.models.Contract;
import tech.bananaz.bot.models.SaleEvent;
import tech.bananaz.bot.utils.KeyUtils;
import tech.bananaz.bot.utils.LooksRareUtils;
import tech.bananaz.bot.utils.OpenseaUtils;
import static java.util.Objects.nonNull;

public class SalesScheduler extends TimerTask {
	
	// Resources declared in Runtime
	private OpenseaUtils api;
	private Contract contract;
	
	// Resources and important
	private boolean active			= false;
	private LooksRareUtils looksApi = new LooksRareUtils();
	private KeyUtils kUtils         = new KeyUtils();
	@Getter
	private String openSeaLastHash  = "";
	@Getter
	private int  previousLooksId 	= 0;
	@Getter
	private long openSeaIdBuffer	= 0;
	private Timer timer 		 	= new Timer(); // creating timer
    private TimerTask task; // creating timer task
	private static final Logger LOGGER = LoggerFactory.getLogger(SalesScheduler.class);

	public SalesScheduler(Contract contract) {
		this.contract = contract;
		try {
			this.api = new OpenseaUtils(this.kUtils.getKey());
		} catch (Exception e) {
			LOGGER.info("No APIKEY provided for OpenSea API {}", contract.getContractAddress());
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		if(nonNull(this.contract) && this.active && this.contract.isActive()) {
			// OpenSea
			try {
				if(!contract.isExcludeOpensea() && nonNull(this.api)) {
					watchSales();
				}
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error(String.format("Failed during get OpenSea sale: %s, stack: %s", this.contract.getContractAddress(), Arrays.toString(e.getStackTrace()))); 
			}
			// LooksRare
			try {
				if(!contract.isExcludeLooks() && !contract.isSlug()) watchLooksRare();
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error(String.format("Failed during get LooksRare sale: %s, stack: %s", this.contract.getContractAddress(), Arrays.toString(e.getStackTrace()))); 
			}
		}
	}

	public boolean start() {
		// Creates a new integer between 1-5 and * by 1000 turns it into a second in milliseconds
		// first random number
		int startsIn = (ThreadLocalRandom.current().nextInt(1, 10)*1000);
		if(nonNull(this.contract)) {
			this.active = true;
			this.task   = this;
			LOGGER.info(String.format("Starting new SalesScheduler in %sms for: %s", startsIn, this.contract.toString()));
			// Starts this new timer, starts at random time and runs per <interval> milliseconds
			this.timer.schedule(task, startsIn , this.contract.getInterval());
		}
		return this.active;
	}
	
	public boolean stop() {
		this.active = false;
		LOGGER.info("Stopping ListingScheduler on " + this.contract.toString());
		return this.active;
	}
	
	private void watchSales() throws Exception {
		// Refresh OpenSea key before every use
		this.api = new OpenseaUtils(this.kUtils.getKey());
		JSONObject marketSales = 
				(!this.contract.isSlug()) ? 
						this.api.getCollectionListed(this.contract.getContractAddress()) :
						this.api.getCollectionListedWithSlug(this.contract.getContractAddress());
		JSONArray assetEvents 	  = (JSONArray) marketSales.get("asset_events");
		
		if(!assetEvents.isEmpty()) {
			ArrayList<SaleEvent> events = new ArrayList<>();
			for(int i = 0; i < assetEvents.size(); i++) {
				// Grab sub-objects in message
				JSONObject newListing   = (JSONObject) assetEvents.get(i);
				long id = Long.valueOf(newListing.getAsString("id"));
				if(id > this.openSeaIdBuffer) {
					SaleEvent event = new SaleEvent(this.contract);
					event.build(newListing);
					// Append to the end of the List
					events.add(event);
				} else break;
			}
			// Only process with data in events
			if(events.size() > 0) {
				//Sort array
				Collections.sort(events);

				if(this.openSeaIdBuffer != 0) {
					// Process sorted array
					for(int i = 0; i < events.size(); i++) {
						SaleEvent event = events.get(i);
						if(this.contract.isShowBundles() || event.getQuantity() == 1) {
							if(event.getId() > this.openSeaIdBuffer && !event.getHash().equalsIgnoreCase(this.openSeaLastHash)) {
								// Log in terminal
								logInfoNewListing(event);

								// Write, ensure not exists to not overwrite existing data
								if(!this.contract.getEvents().existsById(event.getId()))
									this.contract.getEvents().save(event);
							} else break;
						}
					}
				}
				SaleEvent f0 = events.get(0);
				if(f0.getId() > this.openSeaIdBuffer) {
					this.openSeaLastHash = f0.getHash();
					this.openSeaIdBuffer = f0.getId();
				}
			}
			if(events.size() == 0) LOGGER.info(String.format("No sales found this OpenSea loop: %s", this.contract.toString()));
		}
	}
	
	private void watchLooksRare() throws Exception {
		JSONObject payload = this.looksApi.getEvents(this.contract.getContractAddress());
		JSONArray events   = (JSONArray) payload.get("data");
		
		if(!events.isEmpty()) {
			if(this.previousLooksId != 0) {
				for(int i = 0; i < events.size(); i++) {
					// Grab sub-objects in message 
					JSONObject sale = (JSONObject) events.get(i);
					int id = Integer.valueOf(sale.getAsString("id"));
					if(id > this.previousLooksId) {
						// Build event
						SaleEvent event    = new SaleEvent(this.contract);
						event.buildLooksRare(sale);
						
						// Log in terminal
						logInfoNewListing(event);

						// Write, ensure not exists to not overwrite existing data
						if(!this.contract.getEvents().existsById(event.getId()))
							this.contract.getEvents().save(event);
					} else break;
				}
			}
			JSONObject sale = (JSONObject) events.get(0);
			int id = Integer.valueOf(sale.getAsString("id"));
			if(this.previousLooksId == id) LOGGER.info(String.format("No sales found this LooksRare loop: %s", this.contract.toString()));
			previousLooksId = id;
		}
	}
	
	private void logInfoNewListing(SaleEvent event) {
		LOGGER.info("{}, {}", event.toString(),this.contract.toString());
	}
}
