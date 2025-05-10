import axios from 'axios';

interface LoginRequest {
    username: string;
    password: string;
}

export async function login(data: LoginRequest): Promise<string> {
    const res = await axios.post(`${process.env.REACT_APP_API_URL}/api/auth/login`, data);
    return res.data.token;
}