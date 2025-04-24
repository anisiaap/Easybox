import React from 'react'
import styled from 'styled-components'

const HeaderContainer = styled.div`
  background-color: #fff;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #e0e0e0;
`

const Logo = styled.div`
  font-size: 1.5rem;
  font-weight: bold;
`

const HeaderRight = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
`

const Header: React.FC = () => {
    return (
        <HeaderContainer>
            <Logo>Easybox Admin</Logo>
            <HeaderRight>
                {/* Notifications, user avatar, etc. */}
                <div>Alerts</div>
                <div>User Profile</div>
            </HeaderRight>
        </HeaderContainer>
    )
}

export default Header
