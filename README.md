# Air-quality mapping with a drone
A drone air-quality mapping system. The drone's movement is constrained to moving in fixed increments, and only angles of 10. The system retrieves drone air-quality stations, and no-fly-zones as Geo-JSON objects from a webserver. The system then uses these to find an optimal route to pass through all the stations without going into any no-fly-zones.

<hr>
<a href="https://github.com/hwixley/Drone-Route-Planner/actions/workflows/maven.yml"><img src="https://github.com/hwixley/Drone-Route-Planner/actions/workflows/maven.yml/badge.svg"></a>
<hr>

![ilpoutputs](https://user-images.githubusercontent.com/57837950/235266850-71680aa6-7507-4368-934f-7e433d8b5e83.png)

## Drone Route-Planning Algorithms

### Finding the optimal sensor route
In order to decide the optimal sensor route I decided to try lots of different algorithms to find what was optimal in this context.

### Algorithms used
After experience with the travelling salesman problem I had a good idea of what algorithms I wanted to try use for this drone.

#### Initial route setting algorithms: these set a route based purely on distances between sensors.

- **Greedy**<br>
    The Greedy algorithm works by iterating through the sensors the drone needs to visit and
    chooses a route based on which sensor is closest to the last.
    The route is initialized with the first sensor in the list. Thereafter route expansion is done by
    adding the next closest available sensor. This process is continued until a complete route is
    formed.
- **Temperate (custom algorithm)**<br>
    I wanted to create an algorithm that finds optimal paths by prioritizing the sensors which
    have the highest mean distances from other sensors. This prioritization works by creating
    fragments/edges (represents the transition between two sensors) between the sensor with
    the highest mean distance and expanding it with its best possible path (the closest available
    sensor). This is iterated for all sensors from highest to lowest mean distance and placed in a
    priority queue (descending order of mean values). To prevent redundancy in this priority
    queue we only allow a given sensor to be in a maximum of two fragments (as a single sensor
    can be connected to a maximum of two other sensors). Once a priority queue of
    fragments/edges is found then the route can begin to be created. The route is initialized with
    the first fragment in the priority queue. Thereafter route expansion is based upon availability
    in the priority queue. By this I mean that based upon the given sensor we need to expand
    (last sensor in the current route), the algorithm first checks if this sensor can be found in a
    fragment in the priority queue. If so, the other sensor from this given fragment is added to
    the route. Otherwise, the algorithm finds the best available transition for the given sensor
    and adds it to the route. This process is continued until a complete route is formed. In order
    to ensure maximum efficiency redundant fragments (fragments which contain sensors that
    are not available) are deleted when found upon each iteration.
    This algorithm almost works in the opposite way to the greedy algorithm in which rather than
    prioritizing using the shortest distances possible, mine prioritizes not using the longest
    distances possible, this is why I decided to name this algorithm ‘Temperate’ (the antonym of
    greedy).

#### Route refinement algorithms: these naively switch points in the route and see if this improves the overall route cost (total distance travelled).

- **Swap heuristic**<br>
    The Swap heuristic algorithm works by swapping adjacent sensors in the route to see if it improves the cost. This algorithm iterates through each element in the given route to try all possible adjacent swaps. If a single swap in the loop is successful (improves the overall cost) we iterate through the route again (because the route has been changed). If no successful swaps are made throughout an entire loop the algorithm is terminated.

- **2 - Opt heuristic**<br>
    The 2-Opt heuristic algorithm works by flipping the path between two sensors in the route to see if it improves the cost. This algorithm uses a nested loop in order to get two indexes that represent sensors in the route. For every iteration we then try reverse the path between these two sensors. If a single reversal from the entire nested loop was successful (improves the overall cost) we iterate through the route again (because the route has changed). If no successful path reversals are made throughout an entire nested loop the algorithm is terminated.

**NOTE: All of these algorithm implementations can be found in `Algorithms.java` and can be easily swapped between by using the _findOptimalRoute_ () function in `App.java`.

### Results
![ilptable](https://user-images.githubusercontent.com/57837950/235266921-09a837bb-2704-4956-b2ba-8d6dd23bcbc1.png)
![ilpcandles](https://user-images.githubusercontent.com/57837950/235266952-e561075c-e3fc-46da-a9f5-aa75ed2fbc4d.png)
![ilpdistr](https://user-images.githubusercontent.com/57837950/235266994-7c668fb1-64ac-4d71-9cf5-128b8d2f26ae.png)

Given these results, I believe in this context the Greedy -> 2-Opt algorithm would be the most
effective to use. This is evident as it has the lowest standard deviation (excluding Swap given this
result is due to poor performance), the second best minimum number of moves achieved, the best
maximum number of moves achieved, is only 0. 08 moves off the best average, and has the best
worst-case time complexity for all the algorithms with sub 100 move averages.
