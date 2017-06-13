#include <fstream>
#include "TriangulationImpl.h"

using namespace std;

TriangulationImpl::TriangulationImpl():
m_inFile(""), m_outFile("")
{
}

TriangulationImpl::TriangulationImpl(char* inFile, char* outFile)
{
	m_inFile = inFile? inFile: "";
	m_outFile = outFile? outFile: 
		m_inFile.substr(0, m_inFile.find_last_of('.')) + "_test.csmf";
}

TriangulationImpl::~TriangulationImpl()
{
}

void TriangulationImpl::ReadFile(char* inFile, bool oneByOne)
{
	if(inFile)
		m_inFile = inFile;
	std::ifstream in(m_inFile.c_str());
	float x = 0.0;
	float y = 0.0;
	int z = 0;
	int iNumberOfPoints;

	in >> iNumberOfPoints ;

	if(oneByOne)
	{
		for(int i = 0; i < iNumberOfPoints; ++i)
		{
			in >> x;
			in >> y;
			in >> z;
			Point p(x, y);
			m_t.insert(p);
		}
	}
	else
	{
		std::vector<Point> v;
		for(int i = 0; i < iNumberOfPoints; ++i)
		{
			in >> x;
			in >> y;
			in >> z;
			Point p(x, y);
			v.push_back(p);
		}
		vector<Point>::iterator begin = v.begin();
		vector<Point>::iterator end = v.end();
		m_t.insert(begin, end);
	}
	in.close();
}

void TriangulationImpl::WriteSmfFile(char* outFile)
{
	if(outFile)
		m_outFile = outFile;
	std::ofstream out(m_outFile.c_str());
	out << m_t;
	out.close();
}

void TriangulationImpl::RandomConstructionVector(int size)
{
	int delta = 1000;
	double xx = 0.0;
	double yy = 0.0;
	vector<Point> v;

	for(int i = 0; i < size; ++i)
	{
		xx = rand()/(float)RAND_MAX * delta - (delta * 0.1);
		yy = rand()/(float)RAND_MAX * delta - (delta * 0.1);
		v.push_back(Point(xx, yy));
	}
	vector<Point>::const_iterator iter = v.begin();
	vector<Point>::const_iterator end = v.end();
	m_t.insert(iter, end);
}

void TriangulationImpl::RandomConstructionOneByOne(int size)
{
	int delta = 1000;
	double xx = 0.0;
	double yy = 0.0;

	for(int i = 0; i < size; ++i)
	{
		xx = rand()/(float)RAND_MAX * delta - (delta * 0.1);
		yy = rand()/(float)RAND_MAX * delta - (delta * 0.1);
		m_t.insert(Point(xx, yy));
	}
}