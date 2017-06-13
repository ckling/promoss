package algorithms.guards;

public class PolygonGuardMatrix {

	public int m_numElements;
	public int m_numGuards;
	
	//matrix that represents visible elements for each 
	//guard
	protected boolean[][] m_indicesMatrix;
		
	//ctor
	public PolygonGuardMatrix(int numGuards, int numElements)
	{
		m_numElements = numElements;
		m_numGuards = numGuards;
		
		if(m_numElements > 0 && m_numGuards > 0)
		{
			m_indicesMatrix = new boolean[m_numGuards][m_numElements];
		}
		
		//init matrix to false
		for(int i = 0; i < m_numGuards; ++i)
		{
			for(int j = 0; j < m_numElements; ++j)
			{
				m_indicesMatrix[i][j] = false;
			}
		}
	}
	
	//find the number of elements guarded by a guard
	int NumElementsGuarded(int iGuardIndex)
	{
		int result = 0;
		for(int j = 0; j < m_numElements; ++j)
		{
			if(m_indicesMatrix[iGuardIndex][j] == true)
			{
				result++;
			}
		}
		return result;
	}

	//find guard with maximal elements
	int MaxGuardIndex()
	{
		int result = -1;
		int maxNumCover = 0;
		{
			for(int i = 0; i < m_numGuards; ++i)
			{
				int curNumCover = NumElementsGuarded(i);
				if(curNumCover > maxNumCover)
				{
					maxNumCover = curNumCover;
					result = i;
				}
			}
		}
		return result;
	}

	//update indices matrix
	void Update(int sel)
	{
		for(int i = 0; i < m_numGuards; ++i)
		{
			if(i != sel)
			{
				for(int j = 0; j < m_numElements; ++j)
				{
					if(m_indicesMatrix[sel][j] == true)
					{
						m_indicesMatrix[i][j] = false;
					}
				}
			}
		}
		for(int j = 0; j < m_numElements; ++j)
		{
			m_indicesMatrix[sel][j] = false;
		}
	}
	
}
