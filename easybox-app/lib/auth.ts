import * as SecureStore from 'expo-secure-store';

const TOKEN_KEY = 'easybox_token';

export async function saveToken(token: string) {
    await SecureStore.setItemAsync(TOKEN_KEY, token);
}

export async function getToken(): Promise<string | null> {
    return await SecureStore.getItemAsync(TOKEN_KEY);
}

export async function removeToken() {
    await SecureStore.deleteItemAsync(TOKEN_KEY);
}

