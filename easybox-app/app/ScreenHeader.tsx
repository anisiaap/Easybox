import React from 'react';
import { View, StyleSheet } from 'react-native';
import { Text } from 'react-native-paper';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { TouchableOpacity } from 'react-native';

export default function ScreenHeader({
                                         title,
                                         subtitle,
                                         showBack = false,
                                     }: {
    title: string;
    subtitle?: string;
    showBack?: boolean;
}) {
    const router = useRouter();

    return (
        <View style={styles.container}>
            {showBack ? (
                <TouchableOpacity onPress={() => router.back()} style={styles.back}>
                    <Ionicons name="chevron-back" size={22} color="#333" />
                </TouchableOpacity>
            ) : (
                <View style={styles.backPlaceholder} />
            )}

            <View style={styles.titleBlock}>
                <Text style={styles.title} numberOfLines={1}>{title}</Text>
                {subtitle && <Text style={styles.subtitle} numberOfLines={1}>{subtitle}</Text>}
            </View>

            <View style={styles.backPlaceholder} />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        height: 48,
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 12,
        backgroundColor: '#fff',
        borderBottomWidth: 0.5,
        borderBottomColor: '#ddd',
        justifyContent: 'space-between',
    },
    back: {
        width: 40,
        alignItems: 'flex-start',
    },
    backPlaceholder: {
        width: 40,
    },
    titleBlock: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
    },
    title: {
        fontSize: 17,
        fontWeight: '600',
        color: '#111',
    },
    subtitle: {
        fontSize: 12,
        color: '#777',
        marginTop: -2,
    },
});
