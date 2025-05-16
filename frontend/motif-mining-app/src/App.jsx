import React, { useState } from "react";
import axios from "axios";

const datasets = [
  { label: "anneal", path: "/data/anneal.dat" },
  { label: "chess", path: "/data/chess.dat" },
  { label: "connect", path: "/data/connect.dat" },
  { label: "contextPasquier99", path: "/data/contextPasquier99.dat" },
  { label: "eisen", path: "/data/eisen.dat" },
  { label: "heart-cleveland", path: "/data/heart-cleveland.dat" },
  { label: "iris", path: "/data/iris.dat" },
  { label: "mushroom", path: "/data/mushroom.dat" },
  { label: "pumsb", path: "/data/pumsb.dat" },
];
const queryTypes = [
  { id: "frequent", label: "Motifs fréquents" },
  { id: "closed", label: "Motifs fermés" },
  { id: "maximal", label: "Motifs maximaux" },
  { id: "rare", label: "Motifs rares" },
  { id: "generators", label: "Motifs générateurs" },
  { id: "minimal", label: "Motifs minimaux" },
  { id: "size_between", label: "Fermés de taille X-Y" },
  { id: "presence", label: "Fermés avec items présents" },
  { id: "absence", label: "Fermés avec items absents" },
];

export default function App() {
  const [dataset, setDataset] = useState(datasets[0]);
  const [query, setQuery] = useState(queryTypes[0].id);
  const [params, setParams] = useState({});
  const [results, setResults] = useState([]);

  const handleParamChange = (e) => {
    const { name, value } = e.target;
    setParams((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async () => {
    const response = await axios.post("http://localhost:8080/mine", {
      dataset,
      queryType: query,
      params,
    });
    setResults(response.data);
  };

  const renderDynamicFields = () => {
    switch (query) {
      case "frequent":
      case "closed":
      case "maximal":
        return (
          <input
            type="number"
            name="minSupport"
            placeholder="Support minimum (%)"
            onChange={handleParamChange}
            className="border p-2 w-full my-2"
          />
        );
      case "size_between":
        return (
          <>
            <input
              type="number"
              name="minSize"
              placeholder="Taille min"
              onChange={handleParamChange}
              className="border p-2 w-full my-2"
            />
            <input
              type="number"
              name="maxSize"
              placeholder="Taille max"
              onChange={handleParamChange}
              className="border p-2 w-full my-2"
            />
          </>
        );
      case "presence":
      case "absence":
        return (
          <input
            type="text"
            name="items"
            placeholder="Items concernés (ex: 1,3,5)"
            onChange={handleParamChange}
            className="border p-2 w-full my-2"
          />
        );
      default:
        return null;
    }
  };

  return (
    <div className="container">
      <h1 className="text-2xl font-bold mb-4">Fouille de motifs</h1>

      <label>Dataset :</label>
      <select value={dataset} onChange={(e) => setDataset(e.target.value)}>
        {datasets.map((d) => (
          <option key={d.label} value={d.path}>
            {d.label}
          </option>
        ))}
      </select>

      <label>Type de requête :</label>
      <select
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        className="border p-2 w-full my-2"
      >
        {queryTypes.map((q) => (
          <option key={q.id} value={q.id}>
            {q.label}
          </option>
        ))}
      </select>

      <div className="my-4">{renderDynamicFields()}</div>

      <button
        onClick={handleSubmit}
        className="bg-blue-600 text-white px-4 py-2 rounded"
      >
        Lancer la requête
      </button>

      <h2 className="text-xl font-semibold mt-6">Résultats :</h2>
      <table className="w-full border mt-2">
        <thead>
          <tr className="bg-gray-100">
            <th className="p-2 border">Motif</th>
            <th className="p-2 border">Fréquence</th>
          </tr>
        </thead>
        <tbody>
          {results.map((r, i) => (
            <tr key={i}>
              <td className="p-2 border">[{r.pattern.join(", ")}]</td>
              <td className="p-2 border">{r.freq}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
