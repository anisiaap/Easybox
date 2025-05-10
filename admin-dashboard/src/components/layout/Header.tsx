import React from 'react';
import styled from 'styled-components';
import { useNavigate } from 'react-router-dom';

const HeaderContainer = styled.div`
    background-color: #fff;
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    border-bottom: 1px solid #e0e0e0;
`;

const Logo = styled.div`
    font-size: 1.5rem;
    font-weight: bold;
`;

const HeaderRight = styled.div`
    display: flex;
    align-items: center;
    gap: 16px;

    div {
        cursor: pointer;
        color: #333;
        &:hover {
            text-decoration: underline;
        }
    }
`;

const Header: React.FC = () => {
    const navigate = useNavigate();

    const handleLogout = () => {
        localStorage.removeItem('token');
        navigate('/login');
    };

    return (
        <HeaderContainer>
            <Logo>Easybox Admin</Logo>
            <HeaderRight onClick={handleLogout}>
                <div>Log out</div>
            </HeaderRight>
        </HeaderContainer>
    );
};

export default Header;