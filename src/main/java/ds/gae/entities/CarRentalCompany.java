package ds.gae.entities;

import java.io.Serializable;
import java.util.Collection;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.*;

import ds.gae.ReservationException;

@NamedQueries( { 
	
	@NamedQuery(name = "allRegisteredCompanyNames",
				query = "SELECT company.name FROM CarRentalCompany company")
	,@NamedQuery(	name = "carTypesOfCompany",
					query = "SELECT company.carTypes FROM CarRentalCompany company WHERE company.name = :crcName")
		
})


@Entity
public class CarRentalCompany implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(CarRentalCompany.class.getName());
	
	@Id
	private String name;
	@OneToMany(cascade = CascadeType.ALL)
	private Set<Car> cars;
	//@OneToMany(cascade = CascadeType.ALL)
	//private Map<String, CarType> carTypes = new HashMap<String, CarType>();
	@OneToMany(cascade = CascadeType.ALL)
	private Set<CarType> typeSet = new HashSet<CarType>();
	
	/***************
	 * CONSTRUCTOR *
	 ***************/
	public CarRentalCompany() {
		
	}

	public CarRentalCompany(String name, Set<Car> cars) {
		logger.log(Level.INFO, "<{0}> Car Rental Company {0} starting up...", name);
		setName(name);
		this.cars = cars;
		for(Car car:cars)
			typeSet.add(car.getType());
	}

	/********
	 * NAME *
	 ********/

	public String getName() {
		return name;
	}

	private void setName(String name) {
		this.name = name;
	}

	/*************
	 * CAR TYPES *
	 *************/

	public Collection<CarType> getAllCarTypes() {
		return this.typeSet;
	}
	
	public CarType getCarType(String carTypeName) {
		for (Car car: cars) {
			if(car.getType().getName().equals(carTypeName)) {
				return car.getType();
			}
		}
		throw new IllegalArgumentException("<" + carTypeName + "> No car type of name " + carTypeName);
	}
	
	public boolean isAvailable(String carTypeName, Date start, Date end) {
		logger.log(Level.INFO, "<{0}> Checking availability for car type {1}", new Object[]{name, carTypeName});
		Set<String> nameTypeSet = new HashSet<String>();
		CarType theType = null;
		for (CarType type: typeSet) {
			nameTypeSet.add(type.getName());
			if (type.getName().equals(carTypeName)) {
				theType = type;
			}
		}
		if (theType == null) {
			throw new IllegalArgumentException("<" + carTypeName + "> No car type of name " + carTypeName);			
		}
		if(nameTypeSet.contains(carTypeName))
			return getAvailableCarTypes(start, end).contains(theType);
		throw new IllegalArgumentException("<" + carTypeName + "> No car type of name " + carTypeName);
	}
	
	public Set<CarType> getAvailableCarTypes(Date start, Date end) {
		Set<CarType> availableCarTypes = new HashSet<CarType>();
		for (Car car : cars) {
			if (car.isAvailable(start, end)) {
				availableCarTypes.add(car.getType());
			}
		}
		return availableCarTypes;
	}
	
	/*********
	 * CARS *
	 *********/
	
	private Car getCar(int uid) {
		for (Car car : cars) {
			if (car.getId() == uid)
				return car;
		}
		throw new IllegalArgumentException("<" + name + "> No car with uid " + uid);
	}
	
	public Set<Car> getCars() {
    	return this.cars;
    }
	
	private List<Car> getAvailableCars(String carType, Date start, Date end) {
		List<Car> availableCars = new LinkedList<Car>();
		for (Car car : cars) {
			if (car.getType().getName().equals(carType) && car.isAvailable(start, end)) {
				availableCars.add(car);
			}
		}
		return availableCars;
	}

	/****************
	 * RESERVATIONS *
	 ****************/

	public Quote createQuote(ReservationConstraints constraints, String client)
			throws ReservationException {
		logger.log(Level.INFO, "<{0}> Creating tentative reservation for {1} with constraints {2}", 
                        new Object[]{name, client, constraints.toString()});
		
		CarType type = getCarType(constraints.getCarType());
		
		if(!isAvailable(constraints.getCarType(), constraints.getStartDate(), constraints.getEndDate()))
			throw new ReservationException("<" + name
				+ "> No cars available to satisfy the given constraints.");
		
		double price = calculateRentalPrice(type.getRentalPricePerDay(),constraints.getStartDate(), constraints.getEndDate());
		
		return new Quote(client, constraints.getStartDate(), constraints.getEndDate(), getName(), constraints.getCarType(), price);
	}

	// Implementation can be subject to different pricing strategies
	private double calculateRentalPrice(double rentalPricePerDay, Date start, Date end) {
		return rentalPricePerDay * Math.ceil((end.getTime() - start.getTime())
						/ (1000 * 60 * 60 * 24D));
	}

	public Reservation confirmQuote(Quote quote) throws ReservationException {
		logger.log(Level.INFO, "<{0}> Reservation of {1}", new Object[]{name, quote.toString()});
		List<Car> availableCars = getAvailableCars(quote.getCarType(), quote.getStartDate(), quote.getEndDate());
		if(availableCars.isEmpty())
			throw new ReservationException("Reservation failed, all cars of type " + quote.getCarType()
	                + " are unavailable from " + quote.getStartDate() + " to " + quote.getEndDate());
		Car car = availableCars.get((int)(Math.random()*availableCars.size()));
		
		Reservation res = new Reservation(quote, car.getId());
		car.addReservation(res);
		return res;
	}

	public void cancelReservation(Reservation res) {
		logger.log(Level.INFO, "<{0}> Cancelling reservation {1}", new Object[]{name, res.toString()});
		getCar(res.getCarId()).removeReservation(res);
	}
}