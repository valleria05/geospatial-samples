package com.aerospike.delivery;


import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.aerospike.delivery.Database;


public class Job extends Movable implements Comparable<Job> {

  private final Jobs jobs;
  public Jobs.Metadata metadata;
  private int droneId;
  public static int NullID = 0;
  public static int FirstID = 1;
  public final int id;
  public Location origin;
  public Location destination;
  public State state;
  Instant timePutOnHold;
  private Drone drone;
  public int droneid;
  public final ReentrantReadWriteLock lock;
  private boolean isCandidate; // found by the circle query

  public Job(Jobs jobs) {
    super();
    this.jobs = jobs;
    this.id = jobs.nextID++;
    state = State.Init;
    origin = getLocation();
    destination = Location.makeRandom();
    jobs.initMetadata(this);
    lock = new ReentrantReadWriteLock(true);
  }

  public Job(Jobs jobs, Jobs.Metadata metadata, int id, State state,
             Location origin, Location destination, Location location,
             int droneId, boolean isCandidate) {
    super();
    this.jobs = jobs;
    this.metadata = metadata;
    this.id = id;
    this.state = state;
    this.droneId = droneId;
    this.isCandidate = isCandidate;
    this.origin = origin;
    this.destination = destination;
    super.setLocation(location);
    lock = new ReentrantReadWriteLock(true);
  }

  public boolean put() {
    Database.assertWriteLocked(lock);
    return jobs.put(this);
  }


  void updateCoordinates() {
    Database.assertWriteLocked(lock);
    setOrigin  (getDestination());
    assert getLocation().equals(getDestination());
    setDestination(Location.makeRandom());
  }

  @Override
  public void setLocation(Location newValue) {
    Database.assertWriteLocked(lock);
    super.setLocation(newValue);
  }

  public Location getOrigin() {
    return origin;
  }

  public void setOrigin(Location origin) {
    Database.assertWriteLocked(lock);
    this.origin = origin;
  }

  public enum State {
    Init,
    Waiting,
    InProcess,
    OnHold;

    public static State stateForName(Object name) {
      for (State state : State.values()) {
        if (state.name().equals(name)) return state;
      }
      return null;
    }
  }

  public Location getDestination() {
    return destination;
  }

  public void setDestination(Location newValue) {
    Database.assertWriteLocked(lock);
    destination = newValue;
  }

  public State getState() {
    return state;
  }


  public boolean setStateAndPut(Job.State newValue) {
    Database.assertWriteLocked(lock);
    boolean result = changeStateAndPut(state, newValue);
    return result;
  }


  public boolean changeStateAndPut(State from, State to) {
    Database.assertWriteLocked(lock);
    if (to != State.Waiting) {
      isCandidate = false;
    }
    boolean result = jobs.putWithNewState(this, from, to);
    return result;
  }

  public boolean isCandidate() {
    return isCandidate;
  }

  public void setCandidateAndPut(boolean newValue) {
    Database.assertWriteLocked(lock);
    if (isCandidate != newValue) {
      isCandidate = newValue;
      put();
    }
  }

  public Drone getDrone() {

    return drone;
  }

  public void setDrone(Drone newValue) {
    drone = newValue;
  }

  public void touch() {
    timePutOnHold = Instant.now();
  }


  @Override
  public int compareTo(Job o) {
    return new Integer(id).compareTo(o.id);
  }

  @Override
  public String toString() {
    return String.format("%d %s %s %s %s", id, origin, getLocation(), destination, state);
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Job job = (Job) o;
    return id == job.id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  public static class DistanceComparator implements Comparator<Job> {
    private final Location location;

    public DistanceComparator(Location location) {
      super();
      this.location = location;
    }

    @Override
    public int compare(Job o1, Job o2) {
      double distance1 = location.distance(o1.getLocation());
      double distance2 = location.distance(o2.getLocation());
      if (distance1 > distance2) return  1;
      if (distance1 < distance2) return -1;
      return 0;
    }
  }
}