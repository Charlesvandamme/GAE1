package ds.gae;

import java.util.ArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import ds.gae.entities.Car;
import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.CarType;
import ds.gae.entities.Quote;
import ds.gae.entities.Reservation;
import ds.gae.entities.ReservationConstraints;
 
public class CarRentalModel {
	
	private static EntityManager getEntityManager() {
		return EMF.get().createEntityManager();
	}
		
	public Map<String,CarRentalCompany> CRCS = new HashMap<String, CarRentalCompany>();	
	
	private static CarRentalModel instance;
	
	public static CarRentalModel get() {
		if (instance == null)
			instance = new CarRentalModel();
		return instance;
	}
		
	public static boolean companyIsRegistered(String crcName) {
		EntityManager em = EMF.get().createEntityManager();
		CarRentalCompany crc = em.find(CarRentalCompany.class, crcName);
		em.close();
		return (crc != null);
	}
	
	public static void addCarRentalCompany(CarRentalCompany crc) {
		EntityManager em = EMF.get().createEntityManager();
		em.persist(crc);
		em.close();
		
	}
	
	/**
	 * Get the car types available in the given car rental company.
	 *
	 * @param 	crcName
	 * 			the car rental company
	 * @return	The list of car types (i.e. name of car type), available
	 * 			in the given car rental company.
	 */
	public Set<String> getCarTypesNames(String crcName) {
		EntityManager em = getEntityManager();
		CarRentalCompany crc = em.find(CarRentalCompany.class, crcName);
		Set<String> carTypeNames = new HashSet<String>();
		for(CarType cartype: crc.getAllCarTypes())
			carTypeNames.add(cartype.getName());
		em.close();
		return carTypeNames;
	}

    /**
     * Get all registered car rental companies
     *
     * @return	the list of car rental companies
     */
    public Collection<String> getAllRentalCompanyNames() {
    	EntityManager em = getEntityManager();
    	@SuppressWarnings("unchecked")
    	List<String> allRentalCompanyNames = em.createQuery("allRegisteredCompanyNames").getResultList();
    	em.close();
    	return allRentalCompanyNames;
    }
	
	/**
	 * Create a quote according to the given reservation constraints (tentative reservation).
	 * 
	 * @param	company
	 * 			name of the car renter company
	 * @param	renterName 
	 * 			name of the car renter 
	 * @param 	constraints
	 * 			reservation constraints for the quote
	 * @return	The newly created quote.
	 *  
	 * @throws ReservationException
	 * 			No car available that fits the given constraints.
	 */
    public Quote createQuote(String company, String renterName, ReservationConstraints constraints) throws ReservationException {
    	Quote out = null;
    	if (companyIsRegistered(company)) {
    		EntityManager em = EMF.get().createEntityManager();
    		CarRentalCompany crc = em.find(CarRentalCompany.class, company);
    		out = crc.createQuote(constraints, renterName);
            em.close();

        } else {
        	throw new ReservationException("CarRentalCompany not found.");    	
        }
        return out;
    }
    
	/**
	 * Confirm the given quote.
	 *
	 * @param 	q
	 * 			Quote to confirm
	 * 
	 * @throws ReservationException
	 * 			Confirmation of given quote failed.	
	 */
	public void confirmQuote(Quote q) throws ReservationException {
		if (q != null) {
			if (companyIsRegistered(q.getRentalCompany())) {
				EntityManager em = EMF.get().createEntityManager();
				CarRentalCompany crc = em.find(CarRentalCompany.class, q.getRentalCompany());
				crc.confirmQuote(q);
				em.close();
			}
		} else {
			throw new ReservationException("this quote is invalid");
		}

	}
	
    /**
	 * Confirm the given list of quotes
	 * 
	 * @param 	quotes 
	 * 			the quotes to confirm
	 * @return	The list of reservations, resulting from confirming all given quotes.
	 * 
	 * @throws 	ReservationException
	 * 			One of the quotes cannot be confirmed. 
	 * 			Therefore none of the given quotes is confirmed.
	 */
    public List<Reservation> confirmQuotes(List<Quote> quotes) throws ReservationException {    	
    	List<Reservation> reservations = new ArrayList<Reservation>();
		if (quotes != null) {
			for( Quote q : quotes) {	
				if (q != null) {
					if (companyIsRegistered(q.getRentalCompany())) {
						EntityManager em = EMF.get().createEntityManager();
						CarRentalCompany crc = em.find(CarRentalCompany.class, q.getRentalCompany());
						reservations.add(crc.confirmQuote(q));
						em.close();
					}
				}
			}
		} else {
			throw new ReservationException("this quote is invalid");
		}
    	return reservations;
    }
	
	/**
	 * Get all reservations made by the given car renter.
	 *
	 * @param 	renter
	 * 			name of the car renter
	 * @return	the list of reservations of the given car renter
	 */
	public List<Reservation> getReservations(String renter) {
		EntityManager em = EMF.get().createEntityManager();
		@SuppressWarnings("unchecked")
		List<Reservation> out = em.createQuery("reservationsForRenter").setParameter("carRenter", renter).getResultList();
		em.close();
    	
    	return out;
    }

    /**
     * Get the car types available in the given car rental company.
     *
     * @param 	crcName
     * 			the given car rental company
     * @return	The list of car types in the given car rental company.
     */
    public Collection<CarType> getCarTypesOfCarRentalCompany(String crcName) {
    	EntityManager em = EMF.get().createEntityManager();
    	@SuppressWarnings("unchecked")
		Map<String, CarType> types = (Map<String, CarType>) em.createQuery("carTypesOfCompany").setParameter("crcName", crcName);
    	em.close();
    	return types.values();
    }
	
    /**
     * Get the list of cars of the given car type in the given car rental company.
     *
     * @param	crcName
	 * 			name of the car rental company
     * @param 	carType
     * 			the given car type
     * @return	A list of car IDs of cars with the given car type.
     */
    public Collection<Integer> getCarIdsByCarType(String crcName, CarType carType) {
    	Collection<Integer> out = new ArrayList<Integer>();
    	for (Car c : getCarsByCarType(crcName, carType)) {
    		out.add(c.getId());
    	}
    	return out;
    }
    
    /**
     * Get the amount of cars of the given car type in the given car rental company.
     *
     * @param	crcName
	 * 			name of the car rental company
     * @param 	carType
     * 			the given car type
     * @return	A number, representing the amount of cars of the given car type.
     */
    public int getAmountOfCarsByCarType(String crcName, CarType carType) {
    	return this.getCarsByCarType(crcName, carType).size();
    }

	/**
	 * Get the list of cars of the given car type in the given car rental company.
	 *
	 * @param	crcName
	 * 			name of the car rental company
	 * @param 	carType
	 * 			the given car type
	 * @return	List of cars of the given car type
	 */
	private List<Car> getCarsByCarType(String crcName, CarType carType) {				
		EntityManager em = EMF.get().createEntityManager();
		CarRentalCompany crc = em.find(CarRentalCompany.class, crcName);
		List<Car> out = new ArrayList<Car>(); 
		for (Car c : crc.getCars()) {
			if (c.getType() == carType) { 
				out.add(c);
			}
		}
		em.close();
		return out;
	}

	/**
	 * Check whether the given car renter has reservations.
	 *
	 * @param 	renter
	 * 			the car renter
	 * @return	True if the number of reservations of the given car renter is higher than 0.
	 * 			False otherwise.
	 */
	public boolean hasReservations(String renter) {
		return this.getReservations(renter).size() > 0;		
	}	
}