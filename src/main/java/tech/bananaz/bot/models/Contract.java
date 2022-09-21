package tech.bananaz.bot.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import lombok.ToString.Exclude;
import tech.bananaz.repositories.SaleConfigPagingRepository;
import tech.bananaz.repositories.EventPagingRepository;
import tech.bananaz.bot.services.EventScheduler;
import tech.bananaz.enums.RarityEngine;
import tech.bananaz.models.Sale;

@ToString(includeFieldNames=true)
@Data
public class Contract {
	
	@Exclude
	@JsonIgnore
	private EventScheduler newRequest;
	
	@Exclude
	@JsonIgnore
	private SaleConfigPagingRepository configs;
	
	@Exclude
	@JsonIgnore
	private EventPagingRepository events;

	// Pairs from DB definition
	private long id;
	private String contractAddress;
	private int interval;
	private RarityEngine rarityEngine;
	private boolean active 			  = true;

	// OpenSea settings
	private boolean excludeOpensea 	  = false;
	// Support for slug based API requests in OpenSea
	private boolean isSlug 			  = false;
	// For bundles support
	private boolean showBundles 	  = true;
	
	// LooksRare settings
	private boolean excludeLooks 	  = false;
	
	// To save on DB calls
	@Exclude
	@JsonIgnore
	Sale config;

	public void startSalesScheduler() {
		newRequest = new EventScheduler(this);
		newRequest.start();
	}
	
	public void stopSalesScheduler() {
		newRequest.stop();
	}
	
	public boolean getIsSchedulerActive() {
		return this.newRequest.isActive();
	}
	
	public long getLastOpenseaId() {
		return this.newRequest.getOpenSeaIdBuffer();
	}
	
	public long getLastLooksrareId() {
		return this.newRequest.getPreviousLooksId();
	}
	
	public String getLastOpenseaHash() {
		return this.newRequest.getOpenSeaLastHash();
	}
}
