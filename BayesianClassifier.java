/*
 * 
 * 
How to run:
Instantiate BayesianClassifier with inputs in the following order: path to testing folder and path to model file
 *
 *
 */


import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class BayesianClassifier {
	
	LinkedList<Double> productsP = new LinkedList<>();
	LinkedList<Double> productsN = new LinkedList<>();
	HashMap<String, Double> likelihoodsP = new HashMap<>();
	HashMap<String, Double> likelihoodsN = new HashMap<>();
	
	BayesianClassifier(String path1, String path2){
		File inputFolder = new File(path1);
		File[] test_files = inputFolder.listFiles();
		String line = null;
		String[] arr = null;
		
		File model_file = new File(path2);
		try(BufferedReader bfReader = new BufferedReader(new FileReader(model_file))){
			
			//read and store positive data
			while(!(line = bfReader.readLine()).equals("")) {
				arr = line.split(":");
					likelihoodsP.put(arr[0], Double.parseDouble(arr[1]));
			}
						
			while((line = bfReader.readLine()) != null) {
				arr = line.split(":");
				likelihoodsN.put(arr[0], Double.parseDouble(arr[1]));
			}
				

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		//classify the testing reviews using the model
		HashMap<String, Integer> result = classify(test_files);
				
		
		try(PrintWriter pw = new PrintWriter("result.csv")){
			
			StringBuilder sb = new StringBuilder();
			for(Entry e : result.entrySet())
				sb.append(e.getKey() + "," + e.getValue() + "\n");
			
			System.out.println("The result has been printed in result.csv.");
			
			pw.write(sb.toString());
			
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		

		
		
	}
	
	
	//determine how likely the input review is pos/neg
	HashMap<String, Integer> classify(File[] fileList){
		
		HashMap<String, Integer> result = new HashMap<>();
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
							if(likelihoodsP.containsKey(word)) {
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
				result.put(file.getName(), 1);
			else
				result.put(file.getName(), 0);
					
		}

		return result;
	}	
		
		public static void main(String args[]) {
			BayesianClassifier classifier = new BayesianClassifier(args[0], args[1]);
			
		}
}

	
	
	