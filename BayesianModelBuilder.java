import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class BayesianModelBuilder {

	private HashMap<String, Double> posList = new HashMap<>();		//list of words with # of occurrences of each word
	private HashMap<String, Double> negList = new HashMap<>();		//in negative reviews
	private ArrayList<String> features = new ArrayList<>();
	private HashMap<String, Double> likelihoodsP = new HashMap<>();
	private HashMap<String, Double> likelihoodsN = new HashMap<>();
	final static double POS = 900;
	final static double NEG = 900;
	final static double TOTAL = 1800;
	
	
	BayesianModelBuilder(){}

	//take the paths to a folder of text files as input
	BayesianModelBuilder(String path1, String path2){

		//open the positive review folder and list the files in it
		File inputFolder = new File(path1);
		File fileList1[] = inputFolder.listFiles();
		//count occurrences of every word appears in the files
		posList = wordCounter(fileList1);

		//negative folder
		inputFolder = new File(path2);
		File fileList2[] = inputFolder.listFiles();
		//count occurrences of every word appears in the files
		negList = wordCounter(fileList2);
		
		//adjust both lists so they both contain the same words 
		adjustLists(posList, negList);
		
		
	
		//compute info gain
		HashMap<String, Double> IGs = computeIG(posList, negList);
		
		LinkedHashMap<String, Double> sortedIGs = sortList(IGs);
		
		//keep only the useful features in both pos and neg lists 
		selectFeatures(sortedIGs);
		
		
		
		//add the selected words to array list 
		for(Entry e : posList.entrySet())
			features.add((String) e.getKey());
		
		this.likelihoodsP = computeLikelihoods(posList);
		this.likelihoodsN = computeLikelihoods(negList);
		
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("model.txt"));
			for(Entry<String, Double> entry : likelihoodsP.entrySet() ) {
				// put key and value separated by a colon
				bw.write(entry.getKey() + ":"
						+ entry.getValue());
				bw.newLine();
			}
			
			bw.newLine();
			
			for(Entry<String, Double> entry : likelihoodsN.entrySet() ) {
				// put key and value separated by a colon
				bw.write(entry.getKey() + ":"
						+ entry.getValue());
				bw.newLine();
			}

			bw.flush();
			bw.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		
		LinkedList<Integer> result = classify(fileList1);
		LinkedList<Integer> result2 = classify(fileList2);
		System.out.println(getAccuracy(result, result2));
		
		
	}
	
	HashMap<String, Double> getLikelihoodsP(){
		return this.likelihoodsP;
	}
	
	HashMap<String, Double> getLikelihoodsN(){
		return this.likelihoodsN;
	}


	//count the reviews in which each word appears 
	HashMap<String, Double> wordCounter(File[] fileList){
		HashMap<String, Double> wordList = new HashMap<>();
		HashMap<String, Boolean> seenInThisReview = new HashMap<>();
		for(File inputFile : fileList) {
			//initialize seenInThisReview
			for(Entry entry : seenInThisReview.entrySet()) {
				seenInThisReview.put((String) entry.getKey(), false);
			}
			try(BufferedReader bfReader = new BufferedReader(new FileReader(inputFile))){
				String line;
				//read file line by line and tokenize each line into an array of words
				while((line = bfReader.readLine()) != null) {
					String[] strArr = line.split("[^a-zA-Z0-9']+");
					for(String word : strArr) {						
						if(!(word.equals("") || (word.indexOf("'")==word.length()-1) || (word.indexOf("'")==0))) {
							//if the word seen already but not in the current review, increment the counter; add it to map with counter 1
							if(wordList.containsKey(word) && !seenInThisReview.get(word)) 
								wordList.put(word, wordList.get(word)+1);
							else
								wordList.put(word, 1.0);
							seenInThisReview.put(word, true);
						}
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return wordList;
	}
	
	//make both lists contain the same words with a smoothing constant 0.5
	void adjustLists(HashMap<String, Double> list1, HashMap<String, Double> list2) {
		HashMap<String, Double> mp1, mp2;
		if(list1.size() > list2.size()) {
			mp1 = list1;
			mp2 = list2;
		}
		else {
			mp1 = list2;
			mp2 = list1;
		}
		for(Entry e : mp1.entrySet()) {
			String word = (String) e.getKey();
			if(!mp2.containsKey(word))
				mp2.put(word, 0.5);
			else
				mp2.put(word, mp2.get(word)+0.5);
			mp1.put(word, (Double) e.getValue()+0.5);
		}

		for(Entry e : mp2.entrySet()) {
			String word = (String) e.getKey();
			if(!mp1.containsKey(word)) {
				mp1.put(word, 0.5);
				mp2.put(word, (Double) e.getValue()+0.5);
			}
		}
	}
	

	//select useful features by computing gain from mutual information 
	HashMap<String, Double> computeIG(HashMap<String, Double> posList, HashMap<String, Double> negList){
		HashMap<String, Double> IGs = new HashMap<>();
		double ig;

		for(Entry<String, Double> entry : posList.entrySet()) {
			String word = entry.getKey();
			double freqP = entry.getValue();

			double freqN = negList.get(word);
			double miP = (Math.log(freqP*1620.0/(freqP+freqN)*820.0))/Math.log(2);
			double miN = (Math.log(freqN*1620.0/(freqP+freqN)*820.0))/Math.log(2);
			//ig = Math.abs(miP-miN);
			ig = Math.abs((Math.log(freqP/freqN))/Math.log(2));

			IGs.put(word, ig);
		}
		
		
		return IGs;
	}
	
	void selectFeatures(LinkedHashMap<String, Double> IGs){
		
		//choose the top 59% of all the words and remove the rest from pos and neg lists
		Iterator it = IGs.entrySet().iterator();
		for(int i = 0; i < 0.4*IGs.size(); i++) {
			Entry e = (Entry) it.next();
			String word = (String) e.getKey();
			posList.remove(word);
			negList.remove(word);
		}
		
	}
	
	HashMap<String, Double> computeLikelihoods(HashMap<String, Double> list){
		HashMap<String, Double> likelihoods = new HashMap<>();
		
		//likelihood = # of occurrences of a word / total # of occurrences of the occurrences of all the words in a features list 
		for(Entry entry : list.entrySet())
			likelihoods.put((String) entry.getKey(), (Double) entry.getValue()/features.size()*Math.pow(10, 5));
		return likelihoods;
	}
	
	
	//determine how likely the input review is pos/neg
	LinkedList<Integer> classify(File[] fileList){
		
		LinkedList<Integer> result = new LinkedList<>();
		double productP, productN;

		for(File file : fileList){
			productP = productN = 1;
			
			try(BufferedReader bfReader = new BufferedReader(new FileReader(file))){
				String line;
				//read file line by line and tokenize each line into an array of words
				while((line = bfReader.readLine()) != null) {
					String[] strArr = line.split("[^a-zA-Z0-9']+");
					for(String word : strArr) {						
						if(!(word.equals("") || (word.indexOf("'")==word.length()-1) || (word.indexOf("'")==0)))
							//if the word is a feature, get the ratio of P(word|pos) and P(word|neg), and multiply by product 
							if(features.contains(word)) {
								productP *= likelihoodsP.get(word);
								productN *= likelihoodsN.get(word);
							}
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
			
			productP *= Math.pow(10, 40);
			productN *= Math.pow(10, 40);
			
			if(productP > productN)
				result.add(1);
			else
				result.add(0);
					
		}

		return result;
	}	
		
	
	
	double getAccuracy(LinkedList<Integer> result, LinkedList<Integer> result2) {
		double accuracy;
		double sum = 0;
		
		//for positive training samples
		Iterator it = result.iterator();
		while(it.hasNext()) {
			if((Integer) it.next() == 0)
				sum += 1;
		}
		//for negative
		it = result2.iterator();
		while(it.hasNext()) {
			if((Integer) it.next() == 1)
				sum += 1;
		}
		accuracy = 1 - sum/(result.size()+result2.size());
		
		return accuracy;
	}
	
	
	
	
	
	LinkedHashMap<String, Double> sortList(HashMap<String, Double> list) {
		LinkedHashMap<String, Double> sorted = new LinkedHashMap<>();
		list.entrySet().stream().sorted(Map.Entry.comparingByValue())
		.forEachOrdered(x -> sorted.put(x.getKey(), x.getValue()));
		return sorted;
	}
	
	void display(HashMap<String, Double> list) {
		for(Entry e : list.entrySet())
			System.out.println(e.getKey() + ": " + e.getValue());
	}
	
	
	public static void main(String[] args) {
		BayesianModelBuilder model = new BayesianModelBuilder("C:\\Users\\yamar\\Documents\\SJSU\\SP22\\CS156\\156-prog-assignments\\BayesianClassifier\\src\\pos", "C:\\Users\\yamar\\Documents\\SJSU\\SP22\\CS156\\156-prog-assignments\\BayesianClassifier\\src\\neg");
	}


}