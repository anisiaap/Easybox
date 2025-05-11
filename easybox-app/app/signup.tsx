import React, { useState } from 'react';
import {
    View,
    Alert,
    KeyboardAvoidingView,
    Platform,
    StyleSheet,
} from 'react-native';
import {
    TextInput,
    Button,
    Text,
    Card,
    HelperText,
    Title,
} from 'react-native-paper';
import { useRouter } from 'expo-router';
import api from '../lib/api';
import { useNotification } from '../components/NotificationContext';
const PHONE_REGEX = /^07\d{8}$/;
const PWD_REGEX =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.#^])[A-Za-z\d@$!%*?&.#^]{8,}$/;

export default function Signup() {
    const [name, setName] = useState('');
    const [phone, setPhone] = useState('');
    const [password, setPassword] = useState('');
    const [role, setRole] = useState<'client' | 'bakery'>('client');
    const [loading, setLoading] = useState(false);
    const router = useRouter();
    const { notify } = useNotification();
    const isFormValid =
        name.trim() !== '' && PHONE_REGEX.test(phone) && PWD_REGEX.test(password);

    const handleSignup = async () => {
        if (!isFormValid) {
            notify({ type: 'error', message: 'Please correct the errors before submitting.' });
            return;
        }
        try {
            setLoading(true);
            const endpoint = role === 'client' ? '/auth/register-client' : '/auth/register-bakery';
            const payload =
                role === 'client'
                    ? { name, phone, password }
                    : { name, phone, password};

            const res = await api.post(endpoint, payload);

            notify({
                type: 'success',
                message:
                    role === 'client'
                        ? 'Client account created!'
                        : 'Bakery created. Please wait for approval.',
            });
            router.replace('/login');
        } catch (err: any) {
            const msg =
                err?.response?.data?.message ??
                err?.response?.data ??
                'Signup failed. Try again.';
            notify({ type: 'error', message: msg });
        } finally {
            setLoading(false);
        }
    };

    return (
        <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : undefined}
            style={styles.container}
        >
            <Card style={{ padding: 20, borderRadius: 16 }}>
                <Title style={{ textAlign: 'center', marginBottom: 20 }}>Sign Up</Title>

                <TextInput
                    label="Name"
                    value={name}
                    onChangeText={setName}
                    style={{ marginBottom: 12 }}
                    mode="outlined"
                />

                <TextInput
                    label="Phone Number"
                    value={phone}
                    onChangeText={setPhone}
                    keyboardType="phone-pad"
                    mode="outlined"
                    error={phone.length > 0 && !PHONE_REGEX.test(phone)}
                />
                <HelperText type="error" visible={phone.length > 0 && !PHONE_REGEX.test(phone)}>
                    Phone must start with 07 and be 10 digits
                </HelperText>

                <TextInput
                    label="Password"
                    value={password}
                    onChangeText={setPassword}
                    secureTextEntry
                    mode="outlined"
                    error={password.length > 0 && !PWD_REGEX.test(password)}
                />
                <HelperText type="error" visible={password.length > 0 && !PWD_REGEX.test(password)}>
                    Minimum 8 chars, upper‑, lower‑case, number, special
                </HelperText>


                <View style={styles.toggleRow}>
                <>
                    <Button
                        mode={role === 'client' ? 'contained' : 'outlined'}
                        onPress={() => setRole('client')}
                        style={styles.toggleBtn}
                        accessibilityState={{ selected: role === 'client' }} // ✱ CHANGE
                    >
                        Client
                    </Button>
                    <Button
                        mode={role === 'bakery' ? 'contained' : 'outlined'}
                        onPress={() => setRole('bakery')}
                        style={styles.toggleBtn}
                        accessibilityState={{ selected: role === 'bakery' }} // ✱ CHANGE
                    >
                        Bakery
                    </Button>
                </>
                </View>

                <Button
                    mode="contained"
                    onPress={handleSignup}
                    style={{ marginTop: 20 }}
                    disabled={!isFormValid || loading}
                    loading={loading}
                >
                    Create Account
                </Button>
            </Card>
        </KeyboardAvoidingView>
    );
}
const styles = StyleSheet.create({
    container: { flex: 1, justifyContent: 'center', padding: 20 },
    card: { padding: 20, borderRadius: 16 },
    title: { textAlign: 'center', marginBottom: 20 },
    input: { marginBottom: 12 },
    toggleRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginTop: 8,
        height: 44,
    },
    toggleBtn: { flex: 1, marginHorizontal: 4 },
});