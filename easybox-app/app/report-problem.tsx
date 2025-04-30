// import { useLocalSearchParams, useRouter } from 'expo-router';
// import { useState } from 'react';
// import { View, Text, Button, Alert, StyleSheet } from 'react-native';
// import api from '../lib/api';
// import ScreenHeader from './ScreenHeader';
//
// const reasons = ['broken compartment', 'dirty compartment', 'not ok compartment'];
//
// export default function ReportProblem() {
//     const { orderId } = useLocalSearchParams();
//     const [selected, setSelected] = useState<string | null>(null);
//     const router = useRouter();
//
//     const submit = async () => {
//         if (!selected) return Alert.alert('Select a reason');
//         await api.post('/issues/report', { orderId, reason: selected });
//         Alert.alert('Problem reported!');
//         router.back();
//     };
//
//     return (
//         <View style={styles.container}>
//             <ScreenHeader title="Report Problem" showBack />
//             <View style={styles.content}>
//                 <Text style={styles.label}>Order {orderId}</Text>
//                 {reasons.map((r) => (
//                     <View key={r} style={styles.reason}>
//                         <Button
//                             title={r}
//                             color={selected === r ? 'red' : 'gray'}
//                             onPress={() => setSelected(r)}
//                         />
//                     </View>
//                 ))}
//                 <Button title="Submit" onPress={submit} />
//             </View>
//         </View>
//     );
// }
//
// const styles = StyleSheet.create({
//     container: { flex: 1 },
//     content: { padding: 20, gap: 12 },
//     label: { fontWeight: '600', marginBottom: 10 },
//     reason: { marginBottom: 6 },
// });
