package cmsc433.p3;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cmsc433.p3.SkippingMazeSolver.SolutionFound;


//TODO: Delete maze files before submitting

/**
 * This file needs to hold your solver to be tested. 
 * You can alter the class to extend any class that extends MazeSolver.
 * It must have a constructor that takes in a Maze.
 * It must have a solve() method that returns the datatype List<Direction>
 *   which will either be a reference to a list of steps to take or will
 *   be null if the maze cannot be solved.
 */
public class StudentMTMazeSolver extends SkippingMazeSolver
{
	private int cores;
	private Direction solExtra = null;
	
	/**
	 * Helper class for the DFS callable that allows easy access to a 
	 * current choice followed by the next direction for that choice.
	 *
	 */
	public class NextChoice{
		Choice currChoice;
		// nextDirection is the list of previous nodes so they can be added to the
		// solution list when it's found
		LinkedList<Direction> nextDirection;
		
		public NextChoice(Choice currChoice, LinkedList<Direction> nextDirection){
			this.currChoice = currChoice;
			this.nextDirection = nextDirection;
		}
	}
	
	
	/**
	 * Constructor
	 * @param maze
	 */
    public StudentMTMazeSolver(Maze maze)
    {
        super(maze);
        cores = Runtime.getRuntime().availableProcessors();
    }   
	
    /**
     * Solve the maze and return the solution (if there is one).
     */
	public List<Direction> solve() {
	
		ExecutorService executor = Executors.newFixedThreadPool(cores);
		List<Future<List<Direction>>> futures = new LinkedList<Future<List<Direction>>>();
		LinkedList<DFS> workerTasks = new LinkedList<DFS>();
		LinkedList<NextChoice> workers = new LinkedList<NextChoice>();
		
		try {
			Choice startPoint = firstChoice(maze.getStart());
			Choice currChoice = startPoint, newChoice;
			NextChoice nextChoice;
			
			// Place the starting point in the respective directories
			LinkedList<Direction> tempDirections = new LinkedList<Direction>();
			LinkedList<Direction> nextDirections = new LinkedList<Direction>();
			//tempDirections.add(currChoice.choices.peek());
			nextChoice = new NextChoice(startPoint, tempDirections);
			workers.push(nextChoice);
			
			
			// WAYYYYY too much overhead with this while loop. bye bye
			//while (workers.size() < cores && workersCounter < workers.size()){
				nextChoice = workers.removeFirst();
				currChoice = nextChoice.currChoice;
				nextDirections = nextChoice.nextDirection;
				
				int pointNumChoices = currChoice.choices.size();
				// If there are less workers than cores
				if(pointNumChoices + workers.size() < cores){
					for (int i = 0; i < pointNumChoices; i++){		
						newChoice = follow(currChoice.at, currChoice.choices.peek());
						tempDirections = copyLinkedList(nextDirections);
						tempDirections.add(currChoice.choices.pop());
						nextChoice = new NextChoice(newChoice, tempDirections);
						workers.add(nextChoice);
					}
				}
				else {
					workers.add(nextChoice);
				}
			//}
						
			Iterator workerIt = workers.iterator();
			while(workerIt.hasNext()){
				workerTasks.add(new DFS((NextChoice) workerIt.next()));
			}
			
		} 
		// Probably won't execute.
		catch (SolutionFound e){
			e.printStackTrace();
		}
		
		try{
			futures = executor.invokeAll(workerTasks);
		}
		// Something went terribly wrong and this better not happen
		catch (InterruptedException ie){
			ie.printStackTrace();
		}
		
		// No more working for you, sir.
		executor.shutdown();
		
		for(Future<List<Direction>> solution : futures){
			try{
				// Get the solution list if there is one
				if (solution.get() != null){
					return solution.get();
				}
			} 
			// Something went terribly wrong and this better not happen
			catch (InterruptedException | ExecutionException e){
				e.printStackTrace();
			}
		}
		
		// No solution exists.
		return null;
	    
    }

	
	/**
	 * Copies the elements of a LinkedList
	 *
	 */
	private LinkedList copyLinkedList(LinkedList list){
		LinkedList newList = new LinkedList();
		Iterator listIt = list.iterator();
		while(listIt.hasNext())
			newList.add(listIt.next());
		
		return newList;
	}
	

	private class DFS implements Callable<List<Direction>>{
		Choice currentChoice;
		LinkedList<Direction> firstDirection;
		
		public DFS(NextChoice nextChoice){
			currentChoice = nextChoice.currChoice;
			firstDirection = nextChoice.nextDirection;
		}
		
		/**
		 * The original DFS, but the "first" choice is the argument passed into the class
		 */
		public List<Direction> call(){
			LinkedList<Choice> choiceStack = new LinkedList<Choice>();
			Choice ch;
	        
	        try
	        {
	            choiceStack.push(currentChoice);
	            
	            while (!choiceStack.isEmpty())
	            {
	                ch = choiceStack.peek();
	                if (ch.isDeadend())
	                {
	                    // backtrack.
	                    choiceStack.pop();
	                    if (!choiceStack.isEmpty())
	                    	choiceStack.peek().choices.pop();
	                    continue;
	                }
	                choiceStack.push(follow(ch.at, ch.choices.peek()));
	            }
	            // No solution found.
	            return null;
	        }
	        catch (SolutionFound e)
	        {
	        	//System.out.println("solution found");
	            Iterator<Choice> iter = choiceStack.iterator();
	            LinkedList<Direction> solutionPath = new LinkedList<Direction>();
	            while (iter.hasNext())
	            {
	            	ch = iter.next();
	            	// Push the direction to the solution
	            	//System.out.println(ch.choices.peek());
	                solutionPath.push(ch.choices.peek());
	            }
	            // Push the direction to the Solution
	            //solutionPath.push(firstDirection);
	            Iterator dirIter = firstDirection.listIterator(firstDirection.size());
	            while(((ListIterator<Direction>) dirIter).hasPrevious()){
	            	solutionPath.push((Direction) ((ListIterator<Direction>) dirIter).previous());	  
	            }
	            return pathToFullPath(solutionPath);
	        }
		}
	}
}