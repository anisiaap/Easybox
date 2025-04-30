import { useEffect, useState } from 'react';
import { View, Text, Button, FlatList, StyleSheet } from 'react-native';
import api from '../lib/api';
import ScreenHeader from './ScreenHeader';

type Issue = {
    id: string;
    reason: string;
    orderId: string;
    resolution?: string;
};

export default function Maintenance() {
    const [issues, setIssues] = useState<Issue[]>([]);
    const [history, setHistory] = useState<Issue[]>([]);

    const load = async () => {
        const curr = await api.get('/issues/current');
        const past = await api.get('/issues/history');
        setIssues(curr.data);
        setHistory(past.data);
    };

    useEffect(() => { load(); }, []);

    const resolve = async (id: string, resolution: string) => {
        await api.post(`/issues/${id}/resolve`, { resolution });
        load();
    };

    return (
        <View style={styles.container}>
            <ScreenHeader title="Maintenance" />
            <View style={styles.section}>
                <Text style={styles.title}>Current Issues</Text>
                <FlatList
                    data={issues}
                    keyExtractor={(item) => item.id}
                    renderItem={({ item }) => (
                        <View style={styles.card}>
                            <Text>{item.reason} (Order {item.orderId})</Text>
                            <View style={styles.row}>
                                <Button title="Mark Cleaned" onPress={() => resolve(item.id, 'cleaned')} />
                                <Button title="Mark Repaired" onPress={() => resolve(item.id, 'repaired')} />
                            </View>
                        </View>
                    )}
                />
            </View>
            <View style={styles.section}>
                <Text style={styles.title}>Past Maintenance</Text>
                <FlatList
                    data={history}
                    keyExtractor={(item) => item.id}
                    renderItem={({ item }) => (
                        <View style={styles.card}>
                            <Text>{item.reason} → {item.resolution} (Order {item.orderId})</Text>
                        </View>
                    )}
                />
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, paddingBottom: 20 },
    section: { padding: 16 },
    card: { marginBottom: 10, backgroundColor: '#f9f9f9', padding: 10, borderRadius: 6 },
    title: { fontSize: 16, fontWeight: '600', marginBottom: 8 },
    row: { flexDirection: 'row', justifyContent: 'space-between', gap: 8, marginTop: 8 },
});
