#include <TriangulationImpl.h>
#include <CGAL/Timer.h>


/*
	Simple test for CGAL triangulation.
	Input arguments: 
	1. input tsin file path (optional)
	2. output smf file path (optional)

	In case no arguments provided the application will ask for
	number of random points on which the triangulation will be 
	constructed.
*/
int main(int argc, char* argv[]) {

	char* outFile = argc == 3? argv[2]: NULL;
	CGAL::Timer t;

	if(argc > 1) //input file provided
	{
		double triangMS = 0.0;
		TriangulationImpl myTriang(argv[1], outFile);

		//using ifstream iterators
		t.start();
		myTriang.ReadFile(argv[1], true);
		myTriang.WriteSmfFile();
		t.stop();
		triangMS = t.time() * 1000;
		cout << "Read/Write one by one time in ms: " << triangMS << endl;

		//insert one by one
		t.reset();
		t.start();
		myTriang.ReadFile();
		myTriang.WriteSmfFile();
		t.stop();
		triangMS = t.time() * 1000;
		cout << "Read/Write ofstream iterator time in ms: " << triangMS << endl;
	}
	else //Random points time measurement
	{
		//initialize random seed:
		srand(time(NULL));
		int size = 0;
		double rndTriangMS = 0.0;
		TriangulationImpl dt2;
		
		//Get number of points for test
		cout << "Enter number of points for triangulation :";
		cin >> size;

		//Perform randon construction test using std::vector
		t.reset();
		t.start();
		dt2.RandomConstructionVector(size);
		t.stop();
		rndTriangMS = t.time() * 1000;
		cout << "Random construction triangulation time in ms: " << rndTriangMS << endl;

		//Perform randon construction test using one-by-one
		TriangulationImpl dt3;
		t.reset();
		t.start();
		dt3.RandomConstructionOneByOne(size);
		t.stop();
		rndTriangMS = t.time() * 1000;
		cout << "Random construction triangulation one-by-one time in ms: " << rndTriangMS << endl;
	}
	cout << endl << "Press enter to exit...";
	while (cin.get() != '\n')
        continue;                   // clear out non newline characters
    cin.get();   

  return 0;
}
