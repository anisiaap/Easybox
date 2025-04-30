import React, { useState } from 'react';
import { View, Alert, KeyboardAvoidingView, Platform } from 'react-native';
import { TextInput, Button, Text, Card, Title } from 'react-native-paper';
import { useRouter } from 'expo-router';
import api from '../lib/api';
import { saveToken } from '../lib/auth';

export default function Login() {
    const [phone, setPhone] = useState('');
    const [password, setPassword] = useState('');
    const [role, setRole] = useState<'client' | 'bakery'>('client');
    const router = useRouter();

    const handleLogin = async () => {
        try {
            const res = await api.post('/auth/login', {
                phone,
                password,
                role: role.toUpperCase(),
            });
            const token: string = res.data;
            await saveToken(token);
            router.replace({ pathname: '/redirect', params: { role } });
        } catch (e: any) {
            const msg =
                e?.response?.data?.message ||
                e?.response?.data ||
                'Login failed. Check your phone and password.';
            Alert.alert('Login failed', msg);
        }
    };

    return (
        <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : undefined}
            style={{ flex: 1, justifyContent: 'center', padding: 20, backgroundColor: '#f4f4f4' }}
        >
            <Card style={{ padding: 20, borderRadius: 16, elevation: 4 }}>
                <Title style={{ textAlign: 'center', marginBottom: 20 }}>🔐 Login</Title>

                <TextInput
                    label="Phone Number"
                    value={phone}
                    onChangeText={setPhone}
                    keyboardType="phone-pad"
                    autoCapitalize="none"
                    style={{ marginBottom: 16 }}
                    mode="outlined"
                />
                <TextInput
                    label="Password"
                    secureTextEntry
                    value={password}
                    onChangeText={setPassword}
                    style={{ marginBottom: 16 }}
                    mode="outlined"
                />

                <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginBottom: 24 }}>
                    <Button
                        mode={role === 'client' ? 'contained' : 'outlined'}
                        onPress={() => setRole('client')}
                        style={{ flex: 1, marginRight: 8 }}
                    >
                        Client
                    </Button>
                    <Button
                        mode={role === 'bakery' ? 'contained' : 'outlined'}
                        onPress={() => setRole('bakery')}
                        style={{ flex: 1 }}
                    >
                        Bakery
                    </Button>
                </View>

                <Button mode="contained" onPress={handleLogin} style={{ borderRadius: 8 }}>
                    Login
                </Button>

                <Text style={{ marginTop: 20, textAlign: 'center' }}>
                    Don't have an account?{' '}
                    <Text style={{ color: '#2196f3' }} onPress={() => router.push('/signup')}>
                        Sign up
                    </Text>
                </Text>
            </Card>
        </KeyboardAvoidingView>
    );
}

