import React from 'react'
import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import { AiOutlineDashboard, AiOutlineBoxPlot, AiOutlineShoppingCart } from 'react-icons/ai'
import { FaUsers, FaBreadSlice } from 'react-icons/fa'

const SidebarContainer = styled.div`
  width: 240px;
  background-color: #1f1f1f;
  color: #fff;
  display: flex;
  flex-direction: column;
`

const MenuLink = styled(NavLink)`
  display: flex;
  align-items: center;
  padding: 14px 20px;
  text-decoration: none;
  color: #ccc;
  font-weight: 500;

  &.active {
    background-color: #333;
    color: #fff;
  }

  &:hover {
    background-color: #333;
    color: #fff;
  }

  svg {
    margin-right: 8px;
  }
`

const Sidebar: React.FC = () => {
    return (
        <SidebarContainer>
            <MenuLink to="/">
                <AiOutlineDashboard />
                Dashboard
            </MenuLink>
            <MenuLink to="/easyboxes">
                <AiOutlineBoxPlot />
                Easyboxes
            </MenuLink>
            <MenuLink to="/orders">
                <AiOutlineShoppingCart />
                Orders
            </MenuLink>
            <MenuLink to="/customers">
                <FaUsers />
                Customers
            </MenuLink>
            <MenuLink to="/bakeries">
                <FaBreadSlice />
                Bakeries
            </MenuLink>
        </SidebarContainer>
    )
}

export default Sidebar
