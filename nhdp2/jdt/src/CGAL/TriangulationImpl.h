
#include <string>

#include <CGAL/Exact_predicates_inexact_constructions_kernel.h>
#include <CGAL/Triangulation_2.h>

struct K : CGAL::Exact_predicates_inexact_constructions_kernel {};

typedef CGAL::Triangulation_2<K>         Triangulation;
typedef Triangulation::Vertex_circulator Vertex_circulator;
typedef Triangulation::Point             Point;

using namespace std;

/*
This class demonstrates simple CGAL triangulation test
*/
class TriangulationImpl
{
public:
	//ctor
	TriangulationImpl(char* inFile, char* outFile);
	TriangulationImpl();
	//dtor
	~TriangulationImpl();

	//Read tsin file
	void ReadFile(char* inFile = NULL, bool oneByOne = false);
	//Write SMF file
	void WriteSmfFile(char* outFile = NULL);

	//Preform random construction 
	void RandomConstructionVector(int size);
	void RandomConstructionOneByOne(int size);

private:
	string m_inFile;
	string m_outFile;
	//CGAL triangulation
	Triangulation m_t;
};


