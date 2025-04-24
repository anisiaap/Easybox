import React from 'react'
import Header from './Header'
import Sidebar from './Sidebar'
import styled from 'styled-components'

// A simple layout with a sidebar on the left and main content on the right
const LayoutContainer = styled.div`
  display: flex;
  flex-direction: row;
  height: 100vh;
  overflow: hidden;
`

const ContentContainer = styled.div`
  flex: 1;
  background-color: #f5f5f5;
  overflow-y: auto;
`

interface MainLayoutProps {
    children: React.ReactNode
}

const MainLayout: React.FC<MainLayoutProps> = ({ children }) => {
    return (
        <LayoutContainer>
            <Sidebar />
            <ContentContainer>
                <Header />
                {children}
            </ContentContainer>
        </LayoutContainer>
    )
}

export default MainLayout
