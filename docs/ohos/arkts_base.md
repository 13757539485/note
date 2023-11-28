TypeScript的官方教程（https://www.typescriptlang.org/docs/）

在线Playground平台（https://www.typescriptlang.org/play）

### 迭代器
for of和for in
```ts
let list = [4, 5, 6];

for (let i in list) {
    console.log(i); // "0", "1", "2",
}

for (let i of list) {
    console.log(i); // "4", "5", "6"
}
```