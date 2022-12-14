package tech.bananaz.bot.services;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.Getter;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import tech.bananaz.bot.models.Contract;
import tech.bananaz.models.Event;
import tech.bananaz.repositories.EventPagingRepository;
import tech.bananaz.utils.KeyUtils;
import tech.bananaz.utils.LooksRareUtils;
import tech.bananaz.utils.OpenseaUtils;
import tech.bananaz.utils.ParsingUtils;
import static java.util.Objects.nonNull;

public class EventScheduler extends TimerTask {
	
	// Resources declared in Runtime
	private OpenseaUtils api;
	private Contract contract;
	
	// Resources and important
	@Getter
	private boolean active			= false;
	private Timer timer 		 	= new Timer(); // creating timer
	private LooksRareUtils looksApi = new LooksRareUtils();
	private KeyUtils kUtils         = new KeyUtils();
	private ParsingUtils pUtils     = new ParsingUtils();
	private String openSeaKey   	= "0";
	private Long openseaIdBuffer    = Long.valueOf(0);
	private Long looksrareIdBuffer  = Long.valueOf(0);
    private TimerTask task; // creating timer task
	private static final Logger LOGGER = LoggerFactory.getLogger(EventScheduler.class);

	public EventScheduler(Contract contract) {
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
			LOGGER.info(String.format("Starting new EventScheduler in %sms for: %s", startsIn, this.contract.toString()));
			// Starts this new timer, starts at random time and runs per <interval> milliseconds
			this.timer.schedule(task, startsIn , this.contract.getInterval());
		}
		return this.active;
	}
	
	public boolean stop() {
		this.active = false;
		LOGGER.info("Stopping EventScheduler on " + this.contract.toString());
		return this.active;
	}
	
	private void watchSales() throws Exception {
		// Grab events repository
		EventPagingRepository repo = this.contract.getEvents();
		
		// Refresh OpenSea key if we can
		try {
			this.openSeaKey = this.kUtils.getKey();
		} catch (Exception e) {
			LOGGER.error("Failed on OpenSea key get exception {}", e.getMessage());
		}
		
		// Init our OpenSea REST interface
		this.api = new OpenseaUtils(this.openSeaKey);

		// Make GET request for data
		JSONObject marketSales = 
				(!this.contract.isSlug()) ? 
						this.api.getEventsSalesAddress(this.contract.getContractAddress()) :
							this.api.getEventsSalesSlug(this.contract.getContractAddress());
		JSONArray assetEvents = (JSONArray) marketSales.get("asset_events");
		
		// Process the response from OpenSea API
		if(!assetEvents.isEmpty()) {
			ArrayList<Event> events = new ArrayList<>();
			for(int i = 0; i < assetEvents.size(); i++) {
				// Grab sub-objects in message
				JSONObject sale = (JSONObject) assetEvents.get(i);
				// Build event
				Event e = pUtils.buildOpenSeaEvent(
									sale, 
									this.contract.getConfig());
				// Append to the end of the List
				events.add(e);
			}
			// Only process with data in events
			if(events.size() > 0) {
				//Sort array
				Collections.sort(events);

				if(this.openseaIdBuffer == 0) {
					this.openseaIdBuffer = events.get(0).getId();
				}
				
				// To count entries added
				int count = 0;

				// Process sorted array
				for(int i = 0; i < events.size(); i++) {
					Event event = events.get(i);
					if(this.contract.isShowBundles() || event.getQuantity() == 1) {
						if(!repo.existsByHash(event.getHash()) && !repo.existsById(event.getId()) && event.getId() > this.openseaIdBuffer) {
							// Log in terminal
							logInfoNewEvent(event);

							// Write, ensure not exists to not overwrite existing data
							try {
								if(!repo.existsById(event.getId())) {
									repo.save(event);
									count++;
								}
							} catch (Exception ex) {
								LOGGER.error("Error on OpenSea save dispatch of contract id {} with excpetion {} - {}", this.contract.getId(), ex.getCause(), ex.getMessage());
								throw new Exception("Database save error");
							}
							
						}
					}
				}
				if(count == 0) LOGGER.info(String.format("No sales found this OpenSea loop: %s", this.contract.toString()));
			}
		}
	}
	
	private void watchLooksRare() throws Exception {
		// Grab events repository
		EventPagingRepository repo = this.contract.getEvents();
		
		JSONObject payload = this.looksApi.getEventsSalesAddress(this.contract.getContractAddress());
		JSONArray lrEvents   = (JSONArray) payload.get("data");
		
		if(!lrEvents.isEmpty()) {
			ArrayList<Event> events = new ArrayList<>();
			for(int i = 0; i < lrEvents.size(); i++) {
				// Grab sub-objects in message 
				JSONObject sale = (JSONObject) lrEvents.get(i);
				// Build event
				Event e = pUtils.buildLooksRareEvent(
									sale, 
									this.contract.getConfig());

				// Append to the end of the List
				events.add(e);
			}
			
			
			// Only process with data in events
			if(events.size() > 0) {
				//Sort array
				Collections.sort(events);

				if(this.looksrareIdBuffer == 0) {
					this.looksrareIdBuffer = events.get(0).getId();
				}

				// To count entries added
				int count = 0;
				
				// Process sorted array
				for(int i = 0; i < events.size(); i++) {
					Event event = events.get(i);
					if(this.contract.isShowBundles() || event.getQuantity() == 1) {
						if(!repo.existsByHash(event.getHash()) && !repo.existsById(event.getId()) && event.getId() > this.looksrareIdBuffer) {
							// Log in terminal
							logInfoNewEvent(event);

							// Write, ensure not exists to not overwrite existing data
							try {
								if(!repo.existsById(event.getId())) {
									repo.save(event);
									count++;
								}
							} catch (Exception ex) {
								LOGGER.error("Error on LooksRare save dispatch of contract id {} with excpetion {} - {}", this.contract.getId(), ex.getCause(), ex.getMessage());
								throw new Exception("Database save error");
							}
						}
					}
				}
				if(count == 0) LOGGER.info(String.format("No sales found this LooksRare loop: %s", this.contract.toString()));
			}
		}
	}
	
	private void logInfoNewEvent(Event event) {
		LOGGER.info("{}, {}", event.toString(),this.contract.toString());
	}
}
